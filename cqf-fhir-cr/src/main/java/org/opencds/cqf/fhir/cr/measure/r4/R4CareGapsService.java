package org.opencds.cqf.fhir.cr.measure.r4;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.hl7.fhir.r4.model.Factory.newId;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_BUNDLE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_COMPOSITION_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_DETECTED_ISSUE_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_GAP_STATUS_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.CareGapsConstants.CARE_GAPS_REPORT_PROFILE;
import static org.opencds.cqf.fhir.cr.measure.constant.HtmlConstants.HTML_DIV_PARAGRAPH_CONTENT;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_POPULATION_SYSTEM;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils.getFullUrl;
import static org.opencds.cqf.fhir.utility.Resources.newResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DetectedIssue;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.CareGapsProperties;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.enumeration.CareGapsStatusCode;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.builder.BundleBuilder;
import org.opencds.cqf.fhir.utility.builder.CodeableConceptSettings;
import org.opencds.cqf.fhir.utility.builder.CompositionBuilder;
import org.opencds.cqf.fhir.utility.builder.CompositionSectionComponentBuilder;
import org.opencds.cqf.fhir.utility.builder.DetectedIssueBuilder;
import org.opencds.cqf.fhir.utility.builder.NarrativeSettings;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4CareGapsService {

    private static final Logger ourLog = LoggerFactory.getLogger(R4CareGapsService.class);
    private static final Map<String, CodeableConceptSettings> CARE_GAPS_CODES = ImmutableMap.of(
            "http://loinc.org/96315-7",
            new CodeableConceptSettings().add("http://loinc.org", "96315-7", "Gaps in care report"),
            "http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP",
            new CodeableConceptSettings()
                    .add("http://terminology.hl7.org/CodeSystem/v3-ActCode", "CAREGAP", "Care Gaps"));

    private final Repository repository;

    private final MeasureEvaluationOptions measureEvaluationOptions;

    private static final FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);

    private CareGapsProperties careGapsProperties;

    private String serverBase;

    protected final Map<String, Resource> configuredResources = new HashMap<>();

    private final R4MeasureServiceUtils r4MeasureServiceUtils;

    public R4CareGapsService(
            CareGapsProperties careGapsProperties,
            Repository repository,
            MeasureEvaluationOptions measureEvaluationOptions,
            String serverBase) {
        this.repository = repository;
        this.careGapsProperties = careGapsProperties;
        this.measureEvaluationOptions = measureEvaluationOptions;
        this.serverBase = serverBase;

        r4MeasureServiceUtils = new R4MeasureServiceUtils(repository);
    }

    /**
     * Calculate measures describing gaps in care
     *
     * @param periodStart
     * @param periodEnd
     * @param topic
     * @param subject
     * @param practitioner
     * @param organization
     * @param statuses
     * @param measureIds
     * @param measureIdentifiers
     * @param measureUrls
     * @param programs
     * @return Parameters that includes zero to many document bundles that include Care Gap Measure
     *         Reports will be returned.
     */
    public Parameters getCareGapsReport(
            IPrimitiveType<Date> periodStart,
            IPrimitiveType<Date> periodEnd,
            List<String> topic,
            String subject,
            String practitioner,
            String organization,
            List<String> statuses,
            List<String> measureIds,
            List<String> measureIdentifiers,
            List<CanonicalType> measureUrls,
            List<String> programs) {

        validateConfiguration();

        List<Measure> measures = ensureMeasures(r4MeasureServiceUtils.getMeasures(
                measureIds.stream().map(IdType::new).collect(Collectors.toList()),
                measureIdentifiers,
                measureUrls.stream()
                        .map(PrimitiveType::toString)
                        .map(x -> x.replace("CanonicalType[", ""))
                        .map(x -> x.replace("]", ""))
                        .collect(Collectors.toList())));

        List<Patient> patients;
        if (!Strings.isNullOrEmpty(subject)) {
            patients = getPatientListFromSubject(subject);
        } else {
            throw new NotImplementedOperationException("Only the subject parameter has been implemented.");
        }
        throwNotImplementIfPresent(practitioner, "practitioner");
        throwNotImplementIfPresent(organization, "organization");
        listThrowNotImplementIfPresent(programs, "program");
        listThrowNotImplementIfPresent(topic, "topic");
        listThrowIllegalArgumentIfEmpty(statuses, "status");

        checkValidStatusCode(statuses);

        Parameters result = initializeResult();
        patients.forEach(patient -> {
            Parameters.ParametersParameterComponent patientReports = patientReports(
                    periodStart.getValueAsString(), periodEnd.getValueAsString(), patient, statuses, measures);
            if (patientReports != null) {
                result.addParameter(patientReports);
            }
        });
        return result;
    }

    protected void throwNotImplementIfPresent(Object value, String parameterName) {
        if (value != null) {
            throw new NotImplementedOperationException(parameterName + " parameter not implemented");
        }
    }

    protected void listThrowNotImplementIfPresent(List<String> value, String parameterName) {
        if (value != null && !value.isEmpty()) {
            throw new NotImplementedOperationException(parameterName + " parameter not implemented");
        }
    }

    protected void listThrowIllegalArgumentIfEmpty(List<String> value, String parameterName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " parameter requires a value.");
        }
    }

    protected void validateConfiguration() {
        checkNotNull(careGapsProperties, "Setting care-gaps properties are required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(careGapsProperties.getCareGapsReporter()),
                "Setting care-gaps properties.care_gaps_reporter setting is required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(careGapsProperties.getCareGapsCompositionSectionAuthor()),
                "Setting care-gaps properties.care_gaps_composition_section_author is required for the $care-gaps operation.");
        checkArgument(
                !Strings.isNullOrEmpty(serverBase),
                "The fhirBaseUrl setting is required for the $care-gaps operation.");
        Resource configuredReporter = addConfiguredResource(
                Organization.class, careGapsProperties.getCareGapsReporter(), "care_gaps_reporter");
        Resource configuredAuthor = addConfiguredResource(
                Organization.class,
                careGapsProperties.getCareGapsCompositionSectionAuthor(),
                "care_gaps_composition_section_author");

        checkNotNull(
                configuredReporter,
                String.format(
                        "The %s Resource is configured as the CareGapsProperties.care_gaps_reporter but the Resource could not be read.",
                        careGapsProperties.getCareGapsReporter()));
        checkNotNull(
                configuredAuthor,
                String.format(
                        "The %s Resource is configured as the CareGapsProperties.care_gaps_composition_section_author but the Resource could not be read.",
                        careGapsProperties.getCareGapsCompositionSectionAuthor()));
    }

    protected List<Patient> getPatientListFromSubject(String subject) {
        if (subject.startsWith("Patient/")) {
            return Collections.singletonList(validatePatientExists(subject));
        } else if (subject.startsWith("Group/")) {
            return getPatientListFromGroup(subject);
        }
        ourLog.warn("Subject member was not a Patient or a Group, so skipping. \n{}", subject);
        return Collections.emptyList();
    }

    protected List<Patient> getPatientListFromGroup(String subjectGroupId) {
        List<Patient> patientList = new ArrayList<>();
        Group group;
        try {
            group = repository.read(Group.class, newId(subjectGroupId));
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException(Msg.code(2276) + "Could not find Group: " + subjectGroupId);
        }

        group.getMember().forEach(member -> {
            Reference reference = member.getEntity();
            if (reference.getReferenceElement().getResourceType().equals("Patient")) {
                Patient patient = validatePatientExists(reference.getReference());
                patientList.add(patient);
            } else if (reference.getReferenceElement().getResourceType().equals("Group")) {
                patientList.addAll(getPatientListFromGroup(reference.getReference()));
            } else {
                ourLog.info("Group member was not a Patient or a Group, so skipping. \n{}", reference.getReference());
            }
        });

        return patientList;
    }

    protected Patient validatePatientExists(String patientRef) {
        Patient patient;
        try {
            patient = repository.read(Patient.class, new IdType(patientRef));
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException(Msg.code(2277) + "Could not find Patient: " + patientRef);
        }

        return patient;
    }

    protected <T extends Resource> T addConfiguredResource(Class<T> resourceClass, String id, String key) {
        T resource;
        // read resource from repository
        resource = repository.read(resourceClass, new IdType(id));
        // add resource to configured resources
        configuredResources.put(key, resource);
        return resource;
    }

    protected List<Measure> ensureMeasures(List<Measure> measures) {
        measures.forEach(measure -> {
            if (!measure.hasImprovementNotation()) {
                ourLog.info(
                        "Measure '{}' does not specify an improvement notation, defaulting to: '{}'.",
                        measure.getId(),
                        "increase");
            }
        });
        return measures;
    }

    protected Parameters.ParametersParameterComponent patientReports(
            String periodStart, String periodEnd, Patient patient, List<String> statuses, List<Measure> measures) {
        // TODO: add organization to report, if it exists.
        Composition composition = getComposition(patient);
        List<DetectedIssue> detectedIssues = new ArrayList<>();
        Map<String, Resource> evalPlusSDE = new HashMap<>();
        List<MeasureReport> reports = getReports(
                periodStart, periodEnd, patient, statuses, measures, composition, detectedIssues, evalPlusSDE);

        if (reports.isEmpty()) {
            return null;
        }

        return initializePatientParameter(patient)
                .setResource(addBundleEntries(serverBase, composition, detectedIssues, reports, evalPlusSDE));
    }

    protected List<MeasureReport> getReports(
            String periodStart,
            String periodEnd,
            Patient patient,
            List<String> statuses,
            List<Measure> measures,
            Composition composition,
            List<DetectedIssue> detectedIssues,
            Map<String, Resource> evalPlusSDEs) {

        List<MeasureReport> reports = new ArrayList<>();
        MeasureReport report;

        String reportType = MeasureReportType.INDIVIDUAL.toString();
        R4MeasureProcessor r4MeasureProcessor =
                new R4MeasureProcessor(repository, measureEvaluationOptions, new R4RepositorySubjectProvider());

        for (Measure measure : measures) {

            List<String> subjects = Collections.singletonList(Ids.simple(patient));

            report = r4MeasureProcessor.evaluateMeasure(
                    Eithers.forMiddle3(measure.getIdElement()),
                    periodStart,
                    periodEnd,
                    reportType,
                    subjects,
                    null,
                    null);

            if (!report.hasGroup()) {
                ourLog.info(
                        "Report does not include a group so skipping.\nSubject: {}\nMeasure: {}",
                        Ids.simple(patient),
                        Ids.simplePart(measure));
                continue;
            }

            initializeReport(report);

            CareGapsStatusCode gapStatus = getGapStatus(measure, report);
            if (!statuses.contains(gapStatus.toString())) {
                continue;
            }

            DetectedIssue detectedIssue = getDetectedIssue(patient, report, gapStatus);
            detectedIssues.add(detectedIssue);
            composition.addSection(getSection(measure, report, detectedIssue, gapStatus));
            populateEvaluatedResources(report, evalPlusSDEs);
            populateSDEResources(report, evalPlusSDEs);
            reports.add(report);
        }

        return reports;
    }

    protected void initializeReport(MeasureReport measureReport) {
        if (Strings.isNullOrEmpty(measureReport.getId())) {
            IIdType id = Ids.newId(MeasureReport.class, UUID.randomUUID().toString());
            measureReport.setId(id);
        }
        Reference reporter = new Reference().setReference(careGapsProperties.getCareGapsReporter());
        // TODO: figure out what this extension is for
        measureReport.setReporter(reporter);
        if (measureReport.hasMeta()) {
            measureReport.getMeta().addProfile(CARE_GAPS_REPORT_PROFILE);
        } else {
            measureReport.setMeta(new Meta().addProfile(CARE_GAPS_REPORT_PROFILE));
        }
    }

    protected Parameters.ParametersParameterComponent initializePatientParameter(Patient patient) {
        Parameters.ParametersParameterComponent patientParameter = Resources.newBackboneElement(
                        Parameters.ParametersParameterComponent.class)
                .setName("return");
        patientParameter.setId("subject-" + Ids.simplePart(patient));
        return patientParameter;
    }

    protected Bundle addBundleEntries(
            String serverBase,
            Composition composition,
            List<DetectedIssue> detectedIssues,
            List<MeasureReport> measureReports,
            Map<String, Resource> evalPlusSDEs) {
        Bundle reportBundle = getBundle();
        reportBundle.addEntry(getBundleEntry(serverBase, composition));
        measureReports.forEach(report -> reportBundle.addEntry(getBundleEntry(serverBase, report)));
        detectedIssues.forEach(detectedIssue -> reportBundle.addEntry(getBundleEntry(serverBase, detectedIssue)));
        configuredResources.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        evalPlusSDEs.values().forEach(resource -> reportBundle.addEntry(getBundleEntry(serverBase, resource)));
        return reportBundle;
    }

    protected CareGapsStatusCode getGapStatus(Measure measure, MeasureReport measureReport) {
        Pair<String, Boolean> inNumerator = new MutablePair<>("numerator", false);
        Pair<String, Boolean> inDenominator = new MutablePair<>("denominator", false);
        measureReport.getGroup().forEach(group -> group.getPopulation().forEach(population -> {
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inNumerator.getKey())
                    && population.getCount() == 1) {
                inNumerator.setValue(true);
            }
            if (population.hasCode()
                    && population.getCode().hasCoding(MEASUREREPORT_MEASURE_POPULATION_SYSTEM, inDenominator.getKey())
                    && population.getCount() == 1) {
                inDenominator.setValue(true);
            }
        }));
        // default improvementNotation
        boolean isPositive = true;

        // if value is present, set value from measure if populated
        if (measure.hasImprovementNotation()) {
            isPositive =
                    measure.getImprovementNotation().hasCoding(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "increase");
        }

        if (Boolean.FALSE.equals(inDenominator.getValue())) {
            // patient is not in eligible population
            return CareGapsStatusCode.NOT_APPLICABLE;
        }

        if (Boolean.TRUE.equals(inDenominator.getValue())
                && ((isPositive && !inNumerator.getValue()) || (!isPositive && inNumerator.getValue()))) {
            return CareGapsStatusCode.OPEN_GAP;
        }

        return CareGapsStatusCode.CLOSED_GAP;
    }

    protected Bundle.BundleEntryComponent getBundleEntry(String serverBase, Resource resource) {
        return new Bundle.BundleEntryComponent().setResource(resource).setFullUrl(getFullUrl(serverBase, resource));
    }

    protected Composition.SectionComponent getSection(
            Measure measure, MeasureReport measureReport, DetectedIssue detectedIssue, CareGapsStatusCode gapStatus) {
        String narrative = String.format(
                HTML_DIV_PARAGRAPH_CONTENT,
                gapStatus == CareGapsStatusCode.CLOSED_GAP
                        ? "No detected issues."
                        : String.format("Issues detected.  See %s for details.", Ids.simple(detectedIssue)));
        return new CompositionSectionComponentBuilder<>(Composition.SectionComponent.class)
                .withTitle(measure.hasTitle() ? measure.getTitle() : measure.getUrl())
                .withFocus(Ids.simple(measureReport))
                .withText(new NarrativeSettings(narrative))
                .withEntry(Ids.simple(detectedIssue))
                .build();
    }

    protected Bundle getBundle() {
        return new BundleBuilder<>(Bundle.class)
                .withProfile(CARE_GAPS_BUNDLE_PROFILE)
                .withType(Bundle.BundleType.DOCUMENT.toString())
                .build();
    }

    protected Composition getComposition(Patient patient) {
        return new CompositionBuilder<>(Composition.class)
                .withProfile(CARE_GAPS_COMPOSITION_PROFILE)
                .withType(CARE_GAPS_CODES.get("http://loinc.org/96315-7"))
                .withStatus(Composition.CompositionStatus.FINAL.toString())
                .withTitle("Care Gap Report for " + Ids.simplePart(patient))
                .withSubject(Ids.simple(patient))
                .withAuthor(Ids.simple(configuredResources.get("care_gaps_composition_section_author")))
                // .withCustodian(organization) // TODO: Optional: identifies the organization
                // who is responsible for ongoing maintenance of and accessing to this gaps in
                // care report. Add as a setting and optionally read if it's there.
                .build();
    }

    protected DetectedIssue getDetectedIssue(
            Patient patient, MeasureReport measureReport, CareGapsStatusCode careGapStatusCode) {
        return new DetectedIssueBuilder<>(DetectedIssue.class)
                .withProfile(CARE_GAPS_DETECTED_ISSUE_PROFILE)
                .withStatus(DetectedIssue.DetectedIssueStatus.FINAL.toString())
                .withCode(CARE_GAPS_CODES.get("http://terminology.hl7.org/CodeSystem/v3-ActCode/CAREGAP"))
                .withPatient(Ids.simple(patient))
                .withEvidenceDetail(Ids.simple(measureReport))
                .withModifierExtension(new ImmutablePair<>(
                        CARE_GAPS_GAP_STATUS_EXTENSION,
                        new CodeableConceptSettings()
                                .add(
                                        CARE_GAPS_GAP_STATUS_SYSTEM,
                                        careGapStatusCode.toString(),
                                        careGapStatusCode.toDisplayString())))
                .build();
    }

    protected void populateEvaluatedResources(MeasureReport measureReport, Map<String, Resource> resources) {
        measureReport.getEvaluatedResource().forEach(evaluatedResource -> {
            IIdType resourceId = evaluatedResource.getReferenceElement();
            if (resourceId.getResourceType() == null || resources.containsKey(Ids.simple(resourceId))) {
                return;
            }

            Class<? extends IBaseResource> resourceType = fhirContext
                    .getResourceDefinition(resourceId.getResourceType())
                    .newInstance()
                    .getClass();
            IBaseResource resource = repository.read(resourceType, resourceId);

            if (resource instanceof Resource) {
                Resource resourceBase = (Resource) resource;
                resources.put(Ids.simple(resourceId), resourceBase);
            }
        });
    }

    protected void populateSDEResources(MeasureReport measureReport, Map<String, Resource> resources) {
        if (measureReport.hasExtension()) {
            for (Extension extension : measureReport.getExtension()) {
                if (extension.hasUrl() && extension.getUrl().equals(MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION)) {
                    Reference sdeRef = extension.hasValue() && extension.getValue() instanceof Reference
                            ? (Reference) extension.getValue()
                            : null;
                    if (sdeRef != null
                            && sdeRef.hasReference()
                            && !sdeRef.getReference().startsWith("#")) {
                        IdType sdeId = new IdType(sdeRef.getReference());
                        if (!resources.containsKey(Ids.simple(sdeId))) {
                            Class<? extends IBaseResource> resourceType = fhirContext
                                    .getResourceDefinition(sdeId.getResourceType())
                                    .newInstance()
                                    .getClass();
                            IBaseResource resource = repository.read(resourceType, sdeId);
                            if (resource instanceof Resource) {
                                Resource resourceBase = (Resource) resource;
                                resources.put(Ids.simple(sdeId), resourceBase);
                            }
                        }
                    }
                }
            }
        }
    }

    protected Parameters initializeResult() {
        return newResource(Parameters.class, "care-gaps-report-" + UUID.randomUUID());
    }

    protected void checkValidStatusCode(List<String> statuses) {
        for (String status : statuses) {
            if (!status.equals(CareGapsStatusCode.CLOSED_GAP.toString())
                    && !status.equals(CareGapsStatusCode.OPEN_GAP.toString())
                    && !status.equals(CareGapsStatusCode.NOT_APPLICABLE.toString())) {
                throw new RuntimeException("CareGap status parameter: " + status + " is not an accepted value");
            }
        }
    }
}
