package org.opencds.cqf.fhir.cr.group.r4;

import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.param.CompositeParam;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.InternalCodingDt;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.TokenParam;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.cqframework.cql.cql2elm.CqlCompilerOptions;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.model.CompiledLibrary;
import org.cqframework.cql.elm.requirements.fhir.DataRequirementsProcessor;
import org.cqframework.cql.elm.requirements.fhir.utilities.constants.CqlConstants;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.fhir.searchparam.SearchParameterResolver;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;
import org.opencds.cqf.fhir.cr.group.evaluate.EvaluateRequest;
import org.opencds.cqf.fhir.cr.group.evaluate.IEvaluateProcessor;
import org.opencds.cqf.fhir.utility.iterable.BundleMappingIterable;
import org.opencds.cqf.fhir.utility.search.Searches;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EvaluateProcessor implements IEvaluateProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EvaluateProcessor.class);

    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;

    public EvaluateProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.evaluationSettings = evaluationSettings;
    }

    public Group evaluate(EvaluateRequest request) {
        // TODO: Validate that the Group conforms to the CQLGroupDefinition profile?
        // TODO: Validate that the Group conforms to the CRMIComputableGroupDefinition profile?
        // TODO: In theory we should be able to put all this in the adapter layer and do this version independently...
        if (!(request.getGroup() instanceof Group)) {
            throw new IllegalArgumentException("Expected R4 group instance");
        }
        Group groupDefinition = (Group)request.getGroup();
        log.info("Evaluating group definition {}", groupDefinition.getId());

        Group groupResult = new Group();
        var outcome = new OperationOutcome();
        outcome.setId("messages");
        try {

            // TODO: Maybe set an ID?
            // Set a description
            groupResult.setCode(new CodeableConcept().setText(String.format("Group as determined by evaluation of Group definition %s", groupDefinition.getId())));
            // TODO: Get this from the GroupAdapter.url and version if present
            groupResult.addExtension("http://hl7.org/fhir/StructureDefinition/workflow-generatedFrom",new UriType("Group/" + groupDefinition.getId()));
            if (request.getParameters() instanceof Parameters) {
                var inputParameters = ((Parameters) request.getParameters()).copy();
                inputParameters.setId("input-parameters");
                if (request.getSubject() != null) {
                    inputParameters.addParameter("subject", request.getSubject());
                }
                groupResult.addContained(inputParameters);
                groupResult.addExtension("http://hl7.org/fhir/StructureDefinition/cqf-inputParameters", new Reference().setReference("#input-parameters"));
            }

            // Get the expression
            var expressionExtension = groupDefinition.getExtensionByUrl(
                "http://hl7.org/fhir/StructureDefinition/characteristicExpression");
            if (expressionExtension == null || !(expressionExtension.getValue() instanceof Expression)) {
                throw new IllegalArgumentException("Expected a characteristic expression");
            }
            var expression = (Expression)expressionExtension.getValue();

            // TODO: Add support for inline expressions
            if (!expression.hasLanguage() || !expression.getLanguage().startsWith("text/cql-identifier")) {
                throw new UnsupportedOperationException("Group membership evaluation is only supported for cql-identifier expressions at this time.");
            }

            if (expression.getExpression() == null || expression.getExpression().isEmpty()) {
                throw new IllegalArgumentException("Group membership expression is empty");
            }

            var libraryExtensions = groupDefinition.getExtensionsByUrl("http://hl7.org/fhir/StructureDefinition/cqf-library");
            if (libraryExtensions.size() != 1) {
                throw new UnsupportedOperationException("Group membership evaluation is only supported for Groups with one and only one primary library (as identified with a cqf-library extension");
            }
            var libraryUrl = libraryExtensions.get(0).getValueAsPrimitive().getValueAsString();
            if (libraryUrl == null || libraryUrl.isEmpty()) {
                throw new IllegalArgumentException("Could not determine Url of primary library");
            }

            var context = Engines.forRepository(repository, evaluationSettings, null);

            // TODO: This needs to be set to the clients evaluation request timestamp
            var zonedDateTime = ZonedDateTime.now();
            groupResult.addExtension(
                "http://hl7.org/fhir/StructureDefinition/workflow-generatedOn",
                new DateTimeType(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            );

            var parametersConverter = Engines.getCqlFhirParametersConverter(repository.fhirContext());
            var evaluationParameters = parametersConverter.toCqlParameters(request.getParameters());

            // Determine initial membership
            var subjects = getSubjects(request.getSubject(), context, request.getModelResolver(), libraryUrl, expression.getExpression(), evaluationParameters, zonedDateTime);

            var expressions = Set.of(expression.getExpression());

            // Evaluate membership criteria for each subject, and add them to the resulting group
            var subjectResults = new HashSet<String>();
            for (var subject : subjects) {
                var results = (Parameters)request.getLibraryEngine().evaluate(
                    libraryUrl,
                    subject,
                    request.getParameters(),
                    evaluationParameters,
                    request.getData(),
                    zonedDateTime,
                    expressions
                );

                var resultParameter = results.getParameter(expression.getExpression());
                if (resultParameter.hasValue()) {
                    if (!(resultParameter.getValue() instanceof BooleanType)) {
                        outcome.addIssue().setSeverity(IssueSeverity.ERROR).setCode(IssueType.VALUE).setDetails(new CodeableConcept().setText("Expression must evaluate to a boolean"));
                        break;
                    }
                    else if (((BooleanType)resultParameter.getValue()).getValue()) {
                        subjectResults.add(subject);
                    }
                }
            }

            for (var subject : subjectResults) {
                groupResult.addMember().setEntity(new Reference().setReference(subject));
            }
            groupResult.setQuantity(subjectResults.size());
        }
        catch (Exception ex) {
            outcome.addIssue().setSeverity(IssueSeverity.FATAL).setCode(IssueType.EXCEPTION).setDetails(new CodeableConcept().setText(ex.getMessage()));
        }

        if (outcome.hasIssue()) {
            groupResult.addContained(outcome);
            groupResult.addExtension("http://hl7.org/fhir/StructureDefinition/cqf-messages", new Reference().setReference("#messages"));
        }

        return groupResult;
    }

    private String getPatientReference(Resource resource, ModelResolver modelResolver) {
        var reference = modelResolver.resolvePath(resource, (String)modelResolver.getContextPath("Patient", resource.fhirType()));
        if (reference instanceof Reference) {
            return ((Reference)reference).getReference();
        }

        return null;
    }

    private List<String> getSubjects(String subject, CqlEngine context, ModelResolver modelResolver, String libraryUrl, String expression, Map<String, Object> parameters, ZonedDateTime zonedDateTime) {
        if (subject != null && !subject.isEmpty()) {
            return List.of(subject);
        }

        // Attempt to determine initial membership criteria via data requirements inference
        var selectiveDataRequirements = getSelectiveDataRequirements(
            Objects.requireNonNull(context.getEnvironment().getLibraryManager()),
            VersionedIdentifiers.forUrl(libraryUrl),
            expression,
            parameters,
            zonedDateTime
        );

        for (var dr : selectiveDataRequirements) {
            log.info("Processing patient-selective data requirement for {}", dr.getType());
            Multimap<String, List<IQueryParameterType>> searchParams = HashMultimap.create();

            var searchParameterResolver = new SearchParameterResolver(repository.fhirContext());
            for (var codeFilter : dr.getCodeFilter()) {
                log.info("Processing code filter for {}", codeFilter.getPath());
                var sp = searchParameterResolver.getSearchParameterDefinition(dr.getType(), codeFilter.getPath());
                if (sp != null) {
                    log.info("Processing code filter using search parameter {}",
                        sp.getName());
                    if (codeFilter.hasCode()) {
                        List<IQueryParameterType> codeList = new ArrayList<>();
                        for (var code : codeFilter.getCode()) {
                            log.info("Adding code {}", code.getCode());
                            codeList.add(new TokenParam(new InternalCodingDt().setSystem(code.getSystem()).setCode(code.getCode()).setVersion(code.getVersion())));
                        }
                        searchParams.put(sp.getName(), codeList);
                    }
                    else if (codeFilter.hasValueSet()) {
                        var valueSet = codeFilter.getValueSet();
                        // Inline the codes into the retrieve e.g.
                        // Observation?code=system|code,system|code
                        log.info("Adding codes from value set {}", valueSet);
                        List<IQueryParameterType> codeList = new ArrayList<>();
                        for (Code code : context.getEnvironment()
                            .getTerminologyProvider()
                            .expand(new ValueSetInfo().withId(valueSet))) {
                            codeList.add(new TokenParam(new InternalCodingDt()
                                .setSystem(code.getSystem())
                                .setCode(code.getCode())));
                        }
                        searchParams.put(sp.getName(), codeList);
                    }
                    else {
                        log.info("Skipped code filter because it had neither codes nor a value set");
                    }
                }
            }

            for (var dateFilter : dr.getDateFilter()) {
                var sp = searchParameterResolver.getSearchParameterDefinition(dr.getType(), dateFilter.getPath());
                if (sp != null) {
                    if (dateFilter.getValueDateTimeType() != null) {
                        List<IQueryParameterType> dateList = new ArrayList<>();
                        dateList.add(new DateParam(
                            ParamPrefixEnum.EQUAL,  dateFilter.getValueDateTimeType()));
                        searchParams.put(sp.getName(), dateList);
                    }
                    else if (dateFilter.getValuePeriod() != null) {
                        List<IQueryParameterType> dateList = new ArrayList<>();

                        DateParam gte = null;
                        DateParam lte = null;
                        if (dateFilter.getValuePeriod().getStart() != null) {
                            gte = new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, dateFilter.getValuePeriod().getStart());
                        }
                        if (dateFilter.getValuePeriod().getEnd() != null) {
                            lte = new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, dateFilter.getValuePeriod().getEnd());
                        }

                        if (gte != null && lte != null) {
                            searchParams.put(sp.getName(), List.of(new CompositeParam<>(gte, lte)));
                        }
                        else if (gte != null) {
                            searchParams.put(sp.getName(), List.of(gte));
                        }
                        else if (lte != null) {
                            searchParams.put(sp.getName(), List.of(lte));
                        }
                    }
                    else if (dateFilter.getValueDuration() != null) {
                        // TODO: This needs to resolve based on the evaluation request time
                        log.warn("dateFilter with duration is not currently supported");
                    }
                }
            }

            if (!searchParams.isEmpty()) {
                var resourceClass = repository.fhirContext().getResourceDefinition(dr.getType()).getImplementingClass();
                Bundle results = repository.search(Bundle.class, resourceClass, searchParams);

                var subjects = new HashSet<String>();

                // TODO: Refactor to use BundleIterator
                while (results != null) {

                    for (var entry : results.getEntry()) {
                        var reference = getPatientReference(entry.getResource(), modelResolver);
                        if (reference != null) {
                            subjects.add(reference);
                        }
                    }

                    var nextLink = results.getLink("next");
                    if (nextLink != null && nextLink.hasUrl()) {
                        results = repository.link(Bundle.class,
                            nextLink.getUrl());
                    } else {
                        results = null; // No next link, drop out
                    }
                }

                return subjects.stream().toList();
            }
        }

        log.info("Could not determine initial membership via data requirements inference, defaulting to all subjects");
        var bundle = repository.search(Bundle.class, Patient.class, Searches.ALL);
        return new BundleMappingIterable<>(repository, bundle, x -> x.getResource()
            .getIdElement()
            .toUnqualifiedVersionless()
            .getValue())
            .toStream()
            .toList();
    }

    private Library getEffectiveDataRequirements(LibraryManager libraryManager, VersionedIdentifier libraryIdentifier, String expression, Map<String, Object> parameters, ZonedDateTime zonedDateTime) {
        CompiledLibrary compiledLibrary = libraryManager.resolveLibrary(libraryIdentifier);
        DataRequirementsProcessor drp = new DataRequirementsProcessor();
        CqlCompilerOptions o = libraryManager.getCqlCompilerOptions();
        // TODO: This should be a copy of o here, but I can't get IntelliJ to cooperate with me
        o.setAnalyzeDataRequirements(true);
        o.setReportSelectivity(true);
        var r5Library = drp.gatherDataRequirements(
            libraryManager,
            compiledLibrary,
            o,
            Set.of(expression),
            parameters,
            zonedDateTime,
            false,
            true
        );

        VersionConvertor_40_50 converter = new VersionConvertor_40_50(new BaseAdvisor_40_50());
        return (Library)converter.convertResource(r5Library);
    }

    /*
    Returns true if the given resource type has a primary relationship to Patient (i.e. there is
    an element on the resource that is a reference to a Patient, indicating that the presence of
    data could be used as a selective index for patients).
     */
    // TODO: Refactor to model resolver?
    private boolean isPatientSelectiveType(String resourceType) {
        switch (resourceType) {
            case "Account":
            case "AdverseEvent":
            case "AllergyIntolerance":
            case "Appointment":
            case "Basic":
            case "BodyStructure":
            case "CarePlan":
            case "Claim":
            case "ClaimResponse":
            case "ClinicalImpression":
            case "Communication":
            case "CommunicationRequest":
            case "Composition":
            case "Condition":
            case "Consent":
            case "Contract":
            case "Coverage":
            case "DetectedIssue":
            case "Device":
            case "DeviceRequest":
            case "DeviceUseStatement":
            case "DiagnosticReport":
            case "Encounter":
            case "EpisodeOfCare":
            case "ExplanationOfBenefit":
            case "FamilyiMemberHistory":
            case "Flag":
            case "Goal":
            case "GuidanceResponse":
            case "ImagingStudy":
            case "Immunization":
            case "ImmunizationEvaluation":
            case "ImmunizationRecommendation":
            case "MedicationAdministration":
            case "MedicationDispense":
            case "MedicationRequest":
            case "MedicationStatement":
            case "NutritionOrder":
            case "Observation":
            case "Procedure":
            case "QuestionnaireResponse":
            case "RelatedPerson":
            case "RequestGroup":
            case "ResearchSubject":
            case "RiskAssessment":
            case "ServiceRequest":
            case "Specimen":
            case "Task":
            case "VisionPrescription": return true;
            default: return false;
        }
    }

    private List<DataRequirement> getSelectiveDataRequirements(LibraryManager libraryManager, VersionedIdentifier libraryIdentifier, String expression, Map<String, Object> parameters, ZonedDateTime zonedDateTime) {
        var results = new ArrayList<DataRequirement>();
        var effectiveDataRequirements = getEffectiveDataRequirements(libraryManager, libraryIdentifier, expression, parameters, zonedDateTime);
        var selectivity = effectiveDataRequirements.getExtensionsByUrl(CqlConstants.SELECTIVITY_EXT_URL);
        for (var e : selectivity) {
            if (e.getExtensionByUrl("expressionIdentifier").getValueAsPrimitive().getValueAsString().equals(expression)) {
                // For now, pick the first conjunctive, maybe inclusive, patient selective data requirement
                var formExtension = e.getExtensionByUrl("form");
                if (formExtension != null && formExtension.hasValue() && formExtension.getValueAsPrimitive().hasValue() && formExtension.getValueAsPrimitive().getValueAsString().equals("conjunctive")) {
                    var inclusivityExtension = e.getExtensionByUrl("inclusivity");
                    if (inclusivityExtension == null || (inclusivityExtension.hasValue() && inclusivityExtension.getValueAsPrimitive().hasValue() && inclusivityExtension.getValueAsPrimitive().getValueAsString().equals("inclusion"))) {
                        for (var clause : e.getExtensionsByUrl("clause")) {
                            for (var term : clause.getExtensionsByUrl("term")) {
                                if (term.hasValue() && term.getValue() instanceof DataRequirement) {
                                    var dr = (DataRequirement)term.getValue();
                                    if (isPatientSelectiveType(dr.getType()) && dr.hasCodeFilter() || dr.hasDateFilter()) {
                                        results.add(dr);

                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return results;
    }
}
