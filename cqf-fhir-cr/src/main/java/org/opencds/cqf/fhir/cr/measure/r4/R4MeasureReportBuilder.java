package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CRITERIA_REFERENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponentComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureInfo;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;

public class R4MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    protected static final String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    private final MeasureReportScorer<MeasureReport> measureReportScorer;

    public R4MeasureReportBuilder() {
        this.measureReportScorer = new R4MeasureReportScorer();
    }

    private static class BuilderContext {
        private final Measure measure;
        private final MeasureDef measureDef;
        private final MeasureReport measureReport;

        private final HashMap<String, Reference> evaluatedResourceReferences = new HashMap<>();
        private final HashMap<String, Reference> supplementalDataReferences = new HashMap<>();
        private final Map<String, Resource> contained = new HashMap<>();

        public BuilderContext(Measure measure, MeasureDef measureDef, MeasureReport measureReport) {
            this.measure = measure;
            this.measureDef = measureDef;
            this.measureReport = measureReport;
        }

        public Map<String, Resource> contained() {
            return this.contained;
        }

        public void addContained(Resource r) {
            this.contained.putIfAbsent(this.getId(r), r);
        }

        public Measure measure() {
            return this.measure;
        }

        public MeasureReport report() {
            return this.measureReport;
        }

        public MeasureDef measureDef() {
            return this.measureDef;
        }

        public Map<String, Reference> evaluatedResourceReferences() {
            return this.evaluatedResourceReferences;
        }

        public Map<String, Reference> supplementalDataReferences() {
            return this.supplementalDataReferences;
        }

        public Reference addSupplementalDataReference(String id) {
            validateReference(id);
            return this.supplementalDataReferences().computeIfAbsent(id, x -> new Reference(id));
        }

        public Reference addEvaluatedResourceReference(String id) {
            validateReference(id);
            return this.evaluatedResourceReferences().computeIfAbsent(id, x -> new Reference(id));
        }

        public boolean hasEvaluatedResource(String id) {
            validateReference(id);
            return this.evaluatedResourceReferences().containsKey(id);
        }

        public void addCriteriaExtensionToReference(Reference reference, String criteriaId) {
            if (criteriaId == null) throw new AssertionError("CriteriaId is required for extension references");
            var ext = new Extension(EXT_CRITERIA_REFERENCE_URL, new StringType(criteriaId));
            addExtensionIfNotExists(reference, ext);
        }

        public void addCriteriaExtensionToSupplementalData(Resource resource, String criteriaId) {
            var id = getId(resource);

            // This is not an evaluated resource, so add it to the contained resources
            if (!hasEvaluatedResource(id)) {
                this.addContained(resource);
                id = "#" + resource.getIdElement().getIdPart();
            }
            var ref = addSupplementalDataReference(id);
            addCriteriaExtensionToReference(ref, criteriaId);
        }

        public void addCriteriaExtensionToEvaluatedResource(Resource resource, String criteriaId) {
            var id = getId(resource);
            var ref = addEvaluatedResourceReference(id);
            addCriteriaExtensionToReference(ref, criteriaId);
        }

        private String getId(Resource resource) {
            return resource.fhirType() + "/" + resource.getIdElement().getIdPart();
        }

        private void addExtensionIfNotExists(Element element, Extension ext) {
            for (var e : element.getExtension()) {
                if (e.getUrl().equals(ext.getUrl()) && e.getValue().equalsShallow(ext.getValue())) {
                    return;
                }
            }

            element.addExtension(ext);
        }

        private void validateReference(String reference) {
            // Can't be null
            if (reference == null) {
                throw new NullPointerException("validated reference is null");
            }

            // If it's a contained reference, must be just the Guid and nothing else
            if (reference.startsWith("#") && reference.contains("/")) {
                throw new InvalidRequestException("Invalid contained reference: " + reference);
            }

            // If it's a full reference, it must be type/id and that's it
            if (!reference.startsWith("#") && reference.split("/").length != 2) {
                throw new InvalidRequestException("Invalid full reference: " + reference);
            }
        }

        public void addOperationOutcomes() {
            var errorMsgs = this.measureDef.errors();
            for (var error : errorMsgs) {
                addContained(createOperationOutcome(error));
            }
        }

        private OperationOutcome createOperationOutcome(String errorMsg) {
            OperationOutcome op = new OperationOutcome();
            op.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(IssueType.EXCEPTION)
                    .setDiagnostics(errorMsg);
            return op;
        }
    }

    @Override
    public MeasureReport build(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds) {

        var report = this.createMeasureReport(measure, measureDef, measureReportType, subjectIds, measurementPeriod);

        var bc = new BuilderContext(measure, measureDef, report);

        // buildGroups must be run first to set up the builder context to be able to use
        // the evaluatedResource references for SDE processing
        buildGroups(bc);

        buildSDEs(bc);

        addEvaluatedResource(bc);
        addSupplementalData(bc);
        bc.addOperationOutcomes();

        for (var r : bc.contained().values()) {
            bc.report().addContained(r);
        }

        this.measureReportScorer.score(measure.getUrl(), measureDef, bc.report());
        setReportStatus(bc);
        return bc.report();
    }

    private void setReportStatus(BuilderContext bc) {
        if (bc.report().hasContained()
                && bc.report().getContained().stream()
                        .anyMatch(t -> t.getResourceType().equals(ResourceType.OperationOutcome))) {
            // Measure Reports that have encountered an error during evaluation will be set to status 'Error'
            bc.report().setStatus(MeasureReportStatus.ERROR);
        }
    }

    protected void addSupplementalData(BuilderContext bc) {
        var report = bc.report();

        for (Reference r : bc.supplementalDataReferences().values()) {
            report.addExtension(EXT_SDE_REFERENCE_URL, r);
        }
    }

    protected void addEvaluatedResource(BuilderContext bc) {
        var report = bc.report();
        // Only add evaluated resources to individual reports
        if (report.getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.INDIVIDUAL) {
            for (Reference r : bc.evaluatedResourceReferences().values()) {
                report.addEvaluatedResource(r);
            }
        }
    }

    protected void buildGroups(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        var report = bc.report();

        if (measure.getGroup().size() != measureDef.groups().size()) {
            throw new InvalidRequestException(
                    "The Measure has a different number of groups defined than the MeasureDef for Measure: "
                            + measure.getUrl());
        }

        // ASSUMPTION: The groups are in the same order in both the Measure and the
        // MeasureDef
        for (int i = 0; i < measure.getGroup().size(); i++) {
            var measureGroup = measure.getGroup().get(i);
            var defGroup = measureDef.groups().get(i);
            var reportGroup = report.addGroup();
            buildGroup(bc, measureGroup, reportGroup, defGroup);
        }
    }

    private PopulationDef getReportPopulation(GroupDef reportGroup, MeasurePopulationType measurePopType) {
        var populations = reportGroup.populations();
        return populations.stream()
                .filter(e -> e.code().first().code().equals(measurePopType.toCode()))
                .findAny()
                .orElse(null);
    }

    protected void buildGroup(
            BuilderContext bc,
            MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup,
            GroupDef groupDef) {

        var groupDefSizeDiff = 0;
        if (groupDef.populations().stream()
                        .filter(x -> x.type().equals(MeasurePopulationType.DATEOFCOMPLIANCE))
                        .findFirst()
                        .orElse(null)
                != null) {
            // dateOfNonCompliance is another population not calculated
            groupDefSizeDiff = 1;
        }

        if ((measureGroup.getPopulation().size()) != (groupDef.populations().size() - groupDefSizeDiff)) {
            throw new InvalidRequestException(
                    "The MeasureGroup has a different number of populations defined than the GroupDef for Measure: "
                            + bc.measure().getUrl());
        }

        if (measureGroup.getStratifier().size() != (groupDef.stratifiers().size())) {
            throw new InvalidRequestException(
                    "The MeasureGroup has a different number of stratifiers defined than the GroupDef for Measure: "
                            + bc.measure().getUrl());
        }

        reportGroup.setCode(measureGroup.getCode());
        reportGroup.setId(measureGroup.getId());
        // Measure Level Extension
        addMeasureDescription(reportGroup, measureGroup);
        addExtensionImprovementNotation(reportGroup, groupDef);

        for (int i = 0; i < measureGroup.getPopulation().size(); i++) {
            var measurePop = measureGroup.getPopulation().get(i);
            PopulationDef defPop = null;
            for (int x = 0; x < groupDef.populations().size(); x++) {
                var groupDefPop = groupDef.populations().get(x);
                if (groupDefPop
                        .code()
                        .first()
                        .code()
                        .equals(measurePop.getCode().getCodingFirstRep().getCode())) {
                    defPop = groupDefPop;
                    break;
                }
            }
            var reportPop = reportGroup.addPopulation();
            buildPopulation(bc, measurePop, reportPop, defPop, groupDef);
        }

        // add extension to group for totalDenominator and totalNumerator
        if (groupDef.measureScoring().equals(MeasureScoring.PROPORTION)
                || groupDef.measureScoring().equals(MeasureScoring.RATIO)) {

            // add extension to group for
            if (bc.measureReport.getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)) {
                var docPopDef = getReportPopulation(groupDef, DATEOFCOMPLIANCE);
                if (docPopDef != null
                        && docPopDef.getResources() != null
                        && !docPopDef.getResources().isEmpty()) {
                    var docValue = docPopDef.getResources().iterator().next();
                    if (docValue != null) {
                        assert docValue instanceof Interval;
                        Interval docInterval = (Interval) docValue;

                        var helper = new R4DateHelper();
                        reportGroup
                                .addExtension()
                                .setUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                                .setValue(helper.buildMeasurementPeriod((docInterval)));
                    }
                }
            }
        }
        for (int i = 0; i < measureGroup.getStratifier().size(); i++) {
            var groupStrat = measureGroup.getStratifier().get(i);
            var reportStrat = reportGroup.addStratifier();
            var defStrat = groupDef.stratifiers().get(i);
            buildStratifier(bc, groupStrat, reportStrat, defStrat, measureGroup.getPopulation(), groupDef);
        }
    }

    protected void buildStratifier(
            BuilderContext bc,
            MeasureGroupStratifierComponent measureStratifier,
            MeasureReportGroupStratifierComponent reportStratifier,
            StratifierDef stratifierDef,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        // the top level stratifier 'id' and 'code'
        reportStratifier.setCode(Collections.singletonList(measureStratifier.getCode()));
        reportStratifier.setId(measureStratifier.getId());
        // if description is defined, add to MeasureReport
        if (measureStratifier.hasDescription()) {
            reportStratifier.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measureStratifier.getDescription()));
        }

        if (!stratifierDef.components().isEmpty()) {
            // Component Stratifier
            // one or more criteria expression defined, one set of criteria results per component specified
            // results of component stratifier are an intersection of membership to both component result sets

            List<Map<String, CriteriaResult>> subjectCompValues = stratifierDef.components().stream()
                    .map(StratifierComponentDef::getResults)
                    .toList();

            // Stratifiers should be of the same basis as population
            // Split subjects by result values
            // ex. all Male Patients and all Female Patients
            componentStratifier(bc, reportStratifier, populations, groupDef, stratifierDef, subjectCompValues);

        } else {
            // standard Stratifier
            // one criteria expression defined, one set of criteria results
            Map<String, CriteriaResult> subjectValues = stratifierDef.getResults();
            nonComponentStratifier(bc, reportStratifier, populations, groupDef, subjectValues);
        }
    }

    protected void addMeasureDescription(MeasureReportGroupComponent reportGroup, MeasureGroupComponent measureGroup) {
        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measureGroup.getDescription()));
        }
    }

    protected void componentStratifier(
            BuilderContext bc,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef,
            StratifierDef stratifierDef,
            List<Map<String, CriteriaResult>> subjectCompValues) {
        Map<ValueWrapper, List<String>> subjectsByValue = new HashMap<>();
        Map<String, Set<ValueWrapper>> subjectComponents = new HashMap<>();

        // Process each subject map
        for (Map<String, CriteriaResult> subjectMap : subjectCompValues) {
            // Extract and store subject IDs based on their criteria results
            subjectMap.forEach((subjectId, criteriaResult) -> {
                String patientReference = ResourceType.Patient + "/" + subjectId;
                ValueWrapper valueWrapper = new ValueWrapper(criteriaResult.rawValue());

                // Update subjectsByValue map
                subjectsByValue
                        .computeIfAbsent(valueWrapper, k -> new ArrayList<>())
                        .add(patientReference);

                // Update subjectComponents map
                subjectComponents
                        .computeIfAbsent(patientReference, k -> new HashSet<>())
                        .add(valueWrapper);
            });
        }

        // Remove subjects that don’t qualify for all stratifier components
        int componentSize = stratifierDef.components().size();
        subjectComponents.entrySet().removeIf(entry -> entry.getValue().size() < componentSize);

        // Group subjects by unique component sets
        Map<Set<ValueWrapper>, List<String>> componentSubjects = subjectComponents.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        // Build stratums for each unique value set
        componentSubjects.forEach((valueSet, subjects) -> {
            var reportStratum = reportStratifier.addStratum();
            buildStratum(bc, reportStratum, valueSet, subjects, populations, groupDef);
        });
    }

    protected void nonComponentStratifier(
            BuilderContext bc,
            MeasureReportGroupStratifierComponent reportStratifier,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef,
            Map<String, CriteriaResult> subjectValues) {

        Map<ValueWrapper, List<String>> subjectsByValue = subjectValues.keySet().stream()
                .collect(Collectors.groupingBy(
                        x -> new ValueWrapper(subjectValues.get(x).rawValue())));
        // loop through each value key
        for (Map.Entry<ValueWrapper, List<String>> stratValue : subjectsByValue.entrySet()) {
            var reportStratum = reportStratifier.addStratum();
            // patch Patient values with prefix of ResourceType to match with incoming population subjects for stratum
            // intersection
            var patients = stratValue.getValue().stream()
                    .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                    .collect(Collectors.toList());
            // build the stratum for each unique value
            buildStratum(
                    bc, reportStratum, Collections.singleton(stratValue.getKey()), patients, populations, groupDef);
        }
    }

    protected void addExtensionImprovementNotation(MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        // if already set on Measure, don't set on groups too
        if (groupDef.isGroupImprovementNotation()) {
            if (groupDef.isIncreaseImprovementNotation()) {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE_DISPLAY)));
            } else {
                reportGroup.addExtension(
                        MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                        new CodeableConcept(new Coding(
                                MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE,
                                MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE_DISPLAY)));
            }
        }
    }

    private void buildStratum(
            BuilderContext bc,
            StratifierGroupComponent stratum,
            Set<ValueWrapper> values,
            List<String> subjectIds,
            List<MeasureGroupPopulationComponent> populations,
            GroupDef groupDef) {
        boolean isComponent = values.size() > 1;
        for (ValueWrapper value : values) {
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (isComponent) {
                    StratifierGroupComponentComponent sgcc = new StratifierGroupComponentComponent();
                    sgcc.setCode((CodeableConcept) value.getValue());
                    stratum.addComponent(sgcc);
                } else {
                    stratum.setValue((CodeableConcept) value.getValue());
                }
            } else {
                if (isComponent) {
                    StratifierGroupComponentComponent sgcc = new StratifierGroupComponentComponent();
                    sgcc.setCode(new CodeableConcept().setText(value.getValueAsString()));
                    stratum.addComponent(sgcc);
                } else {
                    stratum.setValue(new CodeableConcept().setText(value.getValueAsString()));
                }
            }
        }
        // add each population
        for (MeasureGroupPopulationComponent mgpc : populations) {
            var stratumPopulation = stratum.addPopulation();
            buildStratumPopulation(bc, stratumPopulation, subjectIds, mgpc, groupDef);
        }
    }

    private void buildBooleanBasisStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            PopulationDef populationDef) {
        var popSubjectIds = populationDef.getSubjects().stream()
                .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                .toList();
        if (popSubjectIds.isEmpty()) {
            sgpc.setCount(0);
            return;
        }
        // intersect population subjects to stratifier.value subjects
        Set<String> intersection = new HashSet<>(subjectIds);
        intersection.retainAll(popSubjectIds);
        sgpc.setCount(intersection.size());

        // subject-list ListResource to match intersection of results
        if (!intersection.isEmpty()
                && bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList = this.createIdList(UUID.randomUUID().toString(), intersection);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    private void buildResourceBasisStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            PopulationDef populationDef,
            GroupDef groupDef) {
        var resourceType =
                ResourceType.fromCode(groupDef.getPopulationBasis().code()).toString();
        boolean isResourceType = resourceType != null;
        List<String> resourceIds = new ArrayList<>();
        assert populationDef != null;
        if (populationDef.getSubjectResources() != null) {
            for (String subjectId : subjectIds) {
                // retrieve criteria results by subject Key
                var resources = populationDef
                        .getSubjectResources()
                        .get(subjectId.replace(ResourceType.Patient.toString().concat("/"), ""));
                if (resources != null) {
                    if (isResourceType) {
                        resourceIds.addAll(resources.stream()
                                .map(this::getPopulationResourceIds) // get resource id
                                .toList());
                    } else {
                        resourceIds.addAll(
                                resources.stream().map(Object::toString).toList());
                    }
                }
            }
        }
        if (resourceIds.isEmpty()) {
            sgpc.setCount(0);
            return;
        }

        sgpc.setCount(resourceIds.size());

        // subject-list ListResource to match intersection of results
        if (bc.report().getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUBJECTLIST) {
            ListResource popSubjectList = this.createIdList(UUID.randomUUID().toString(), resourceIds);
            bc.addContained(popSubjectList);
            sgpc.setSubjectResults(new Reference("#" + popSubjectList.getId()));
        }
    }

    private void buildStratumPopulation(
            BuilderContext bc,
            StratifierGroupPopulationComponent sgpc,
            List<String> subjectIds,
            MeasureGroupPopulationComponent population,
            GroupDef groupDef) {
        sgpc.setCode(population.getCode());
        sgpc.setId(population.getId());

        if (population.hasDescription()) {
            sgpc.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(population.getDescription()));
        }

        var populationDef = groupDef.populations().stream()
                .filter(t -> t.code()
                        .codes()
                        .get(0)
                        .code()
                        .equals(population.getCode().getCodingFirstRep().getCode()))
                .findFirst()
                .orElse(null);
        assert populationDef != null;
        if (groupDef.isBooleanBasis()) {
            buildBooleanBasisStratumPopulation(bc, sgpc, subjectIds, populationDef);
        } else {
            buildResourceBasisStratumPopulation(bc, sgpc, subjectIds, populationDef, groupDef);
        }
    }

    protected String getPopulationResourceIds(Object resourceObject) {
        var resource = (Resource) resourceObject;
        return resource.getId();
    }

    private void buildPopulation(
            BuilderContext bc,
            MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation,
            PopulationDef populationDef,
            GroupDef groupDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());

        if (groupDef.isBooleanBasis()) {
            reportPopulation.setCount(populationDef.getSubjects().size());
        } else {
            reportPopulation.setCount(populationDef.getResources().size());
        }

        if (measurePopulation.hasDescription()) {
            reportPopulation.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL,
                    new StringType(measurePopulation.getDescription()));
        }

        addEvaluatedResourceReferences(bc, populationDef.id(), populationDef.getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        // subjectResult set defined by basis of Measure
        Set<String> populationSet;
        if (groupDef.isBooleanBasis()) {
            populationSet = populationDef.getSubjects().stream()
                    .map(t -> ResourceType.Patient.toString().concat("/").concat(t))
                    .collect(Collectors.toSet());
        } else {
            populationSet = populationDef.getResources().stream()
                    .filter(Resource.class::isInstance)
                    .map(this::getPopulationResourceIds)
                    .collect(Collectors.toSet());
        }

        measurePopulation.setUserData(POPULATION_SUBJECT_SET, populationSet);

        // Report Type behavior
        if (Objects.requireNonNull(bc.report().getType()) == MeasureReport.MeasureReportType.SUBJECTLIST
                && !populationSet.isEmpty()) {
            ListResource subjectList = createIdList(UUID.randomUUID().toString(), populationSet);
            bc.addContained(subjectList);
            reportPopulation.setSubjectResults(new Reference("#" + subjectList.getId()));
        }

        // Population Type behavior
        if (Objects.requireNonNull(populationDef.type()) == MeasurePopulationType.MEASUREOBSERVATION) {
            buildMeasureObservations(bc, populationDef.expression(), populationDef.getResources());
        }
    }

    protected void buildMeasureObservations(BuilderContext bc, String observationName, Set<Object> resources) {
        for (int i = 0; i < resources.size(); i++) {
            // TODO: Do something with the resource...
            Observation observation = createMeasureObservation(
                    bc, "measure-observation-" + observationName + "-" + (i + 1), observationName);
            bc.addContained(observation);
        }
    }

    protected ListResource createList(String id) {
        ListResource list = new ListResource();
        list.setId(id);
        return list;
    }

    protected ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(id, ids.stream().map(Reference::new).collect(Collectors.toList()));
    }

    protected ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    protected void addEvaluatedResourceReferences(
            BuilderContext bc, String criteriaId, Set<Object> evaluatedResources) {
        if (evaluatedResources == null || evaluatedResources.isEmpty()) {
            return;
        }

        for (Object object : evaluatedResources) {
            Resource resource = (Resource) object;
            bc.addCriteriaExtensionToEvaluatedResource(resource, criteriaId);
        }
    }

    // This processes the SDEs for a given report.
    // Case 1: individual - primitive types (ints, codes, etc)
    // convert to observation, add observation as contained, add sde reference with
    // criteria reference extension
    // Case 2: individual - resource types
    // add sde reference with criteria reference extension for each resource
    // if not an evaluated resource, add to contained
    // Case 3: population - primitive types, non aggregatable
    // convert to observation, add observation as contained, add sde reference with
    // criteria reference extension,
    // Case 4: population - primitive type, aggregatable
    // aggregate by value, convert to observation, add observation as contained, sum
    // the
    // sde reference with criteria reference extension
    // Case 5: population - resource types
    // add sde reference with criteria reference extension for each resource
    // if not an evaluated resource, add to contained
    protected void buildSDE(BuilderContext bc, SdeDef sde) {
        var report = bc.report();

        // No SDEs were calculated, do nothing
        if (sde.getResults().isEmpty()) {
            return;
        }

        // Add all evaluated resources
        for (var e : sde.getResults().entrySet()) {
            addEvaluatedResourceReferences(bc, sde.id(), e.getValue().evaluatedResources());
        }

        CodeableConcept concept = conceptDefToConcept(sde.code());

        Map<ValueWrapper, Long> accumulated = sde.getResults().values().stream()
                .flatMap(x -> Lists.newArrayList(x.iterableValue()).stream())
                .filter(Objects::nonNull)
                .map(ValueWrapper::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Map.Entry<ValueWrapper, Long> accumulator : accumulated.entrySet()) {

            Resource obs;
            if (!(accumulator.getKey().getValue() instanceof Resource)) {
                String valueCode = accumulator.getKey().getValueAsString();
                Long valueCount = accumulator.getValue();

                Coding valueCoding = new Coding().setCode(valueCode);

                if (Objects.requireNonNull(report.getType()) == MeasureReport.MeasureReportType.INDIVIDUAL) {
                    obs = createPatientObservation(bc, UUID.randomUUID().toString(), sde.id(), valueCoding, concept);
                } else {
                    obs = createPopulationObservation(
                            bc, UUID.randomUUID().toString(), sde.id(), valueCoding, valueCount, concept);
                }

                bc.addCriteriaExtensionToSupplementalData(obs, sde.id());
            } else {
                Resource r = (Resource) accumulator.getKey().getValue();
                bc.addCriteriaExtensionToSupplementalData(r, sde.id());
            }
        }
    }

    protected void buildSDEs(BuilderContext bc) {
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        // ASSUMPTION: Measure SDEs are in the same order as MeasureDef SDEs
        for (int i = 0; i < measure.getSupplementalData().size(); i++) {
            var sde = measureDef.sdes().get(i);
            buildSDE(bc, sde);
        }
    }

    private CodeableConcept conceptDefToConcept(ConceptDef c) {
        var cc = new CodeableConcept().setText(c.text());
        for (var cd : c.codes()) {
            cc.addCoding(codeDefToCoding(cd));
        }

        return cc;
    }

    private Coding codeDefToCoding(CodeDef c) {
        var cd = new Coding();
        cd.setSystem(c.system());
        cd.setCode(c.code());
        cd.setVersion(c.version());
        cd.setDisplay(c.display());

        return cd;
    }

    protected MeasureReport createMeasureReport(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType type,
            List<String> subjectIds,
            Interval measurementPeriod) {
        MeasureReport report = new MeasureReport();
        report.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
        report.setType(org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.fromCode(type.toCode()));

        if (type == MeasureReportType.INDIVIDUAL && !subjectIds.isEmpty()) {
            report.setSubject(new Reference(subjectIds.get(0)));
        }
        var helper = new R4DateHelper();
        if (measurementPeriod != null) {
            report.setPeriod(helper.buildMeasurementPeriod((measurementPeriod)));
        }

        report.setMeasure(getMeasure(measure));
        report.setDate(new java.util.Date());
        report.setImplicitRules(measure.getImplicitRules());
        if (measureDef.groups().isEmpty() || !measureDef.groups().get(0).isGroupImprovementNotation()) {
            // if true, all group components have the same improvement Notation
            report.setImprovementNotation(measure.getImprovementNotation());
        }
        report.setLanguage(measure.getLanguage());

        if (measure.hasDescription()) {
            report.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measure.getDescription()));
        }

        return report;
    }

    protected Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure =
                new Extension().setUrl(MeasureInfo.MEASURE).setValue(new CanonicalType(measureInfo.getMeasure()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);

        return obsExtension;
    }

    private String getMeasure(Measure measure) {
        if (StringUtils.isNotBlank(measure.getUrl()) && !measure.getUrl().contains("|") && measure.hasVersion()) {
            return measure.getUrl() + "|" + measure.getVersion();
        }
        return measure.getUrl();
    }

    private Coding supplementalDataCoding;

    private Coding geSupplementalDataCoding() {
        if (supplementalDataCoding == null) {
            supplementalDataCoding = new Coding()
                    .setCode("supplemental-data")
                    .setSystem("http://terminology.hl7.org/CodeSystem/measure-data-usage");
        }
        return supplementalDataCoding;
    }

    private CodeableConcept getMeasureUsageConcept(CodeableConcept originalConcept) {
        CodeableConcept measureUsageConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(geSupplementalDataCoding());
        measureUsageConcept.setCoding(list);

        if (originalConcept != null) {
            if (originalConcept.hasText() && StringUtils.isNotBlank(originalConcept.getText())) {
                measureUsageConcept.setText(originalConcept.getText());
            }
            if (originalConcept.hasCoding()) {
                measureUsageConcept.getCoding().add(originalConcept.getCodingFirstRep());
            }
        }
        return measureUsageConcept;
    }

    protected DomainResource createPopulationObservation(
            BuilderContext bc,
            String id,
            String populationId,
            Coding valueCoding,
            Long sdeAccumulatorValue,
            CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        CodeableConcept obsCodeableConcept = new CodeableConcept();
        List<Coding> list = new ArrayList<>();
        list.add(valueCoding);
        if (originalConcept != null && originalConcept.hasCoding()) {
            list.add(originalConcept.getCodingFirstRep());
        }
        obsCodeableConcept.setCoding(list);

        obs.setCode(obsCodeableConcept);
        obs.setValue(new IntegerType(sdeAccumulatorValue));

        return obs;
    }

    protected DomainResource createPatientObservation(
            BuilderContext bc, String id, String populationId, Coding valueCoding, CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        obs.setCode(getMeasureUsageConcept(originalConcept));

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    protected Observation createObservation(BuilderContext bc, String id, String populationId) {
        var measure = bc.measure();
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(
                        measure.hasUrl()
                                ? measure.getUrl()
                                : (measure.hasId()
                                        ? MeasureInfo.MEASURE_PREFIX
                                                + measure.getIdElement().getIdPart()
                                        : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    protected Observation createMeasureObservation(BuilderContext bc, String id, String observationName) {
        Observation obs = this.createObservation(bc, id, observationName);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        return obs;
    }

    // This is some hackery because most of these objects don't implement
    // hashCode or equals, meaning it's hard to detect distinct values;
    class ValueWrapper {
        protected Object value;

        public ValueWrapper(Object value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return this.getKey().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (this.getClass() != o.getClass()) return false;

            ValueWrapper other = (ValueWrapper) o;

            if (other.getValue() == null ^ this.getValue() == null) {
                return false;
            }

            if (other.getValue() == null && this.getValue() == null) {
                return true;
            }

            return this.getKey().equals(other.getKey());
        }

        public String getKey() {
            String key = null;
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                // ASSUMPTION: We won't have different systems with the same code
                // within a given stratifier / sde
                key = joinValues("coding", c.getCode());
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                key = joinValues("codeable-concept", c.getCodingFirstRep().getCode());
            } else if (value instanceof Code) {
                Code c = (Code) value;
                key = joinValues("code", c.getCode());
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                key = joinValues("enum", e.toString());
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                key = joinValues("primitive", p.getValueAsString());
            } else if (value instanceof Identifier) {
                key = ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                key = ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                key = value.toString();
            }

            if (key == null) {
                throw new InvalidRequestException(String.format("found a null key for the wrapped value: %s", value));
            }

            return key;
        }

        public String getValueAsString() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return "<null>";
            }
        }

        public String getDescription() {
            if (value instanceof Coding) {
                Coding c = ((Coding) value);
                return c.hasDisplay() ? c.getDisplay() : c.getCode();
            } else if (value instanceof CodeableConcept) {
                CodeableConcept c = ((CodeableConcept) value);
                return c.getCodingFirstRep().hasDisplay()
                        ? c.getCodingFirstRep().getDisplay()
                        : c.getCodingFirstRep().getCode();
            } else if (value instanceof Code) {
                Code c = (Code) value;
                return c.getDisplay() != null ? c.getDisplay() : c.getCode();
            } else if (value instanceof Enum) {
                Enum<?> e = (Enum<?>) value;
                return e.toString();
            } else if (value instanceof IPrimitiveType<?>) {
                IPrimitiveType<?> p = (IPrimitiveType<?>) value;
                return p.getValueAsString();
            } else if (value instanceof Identifier) {
                return ((Identifier) value).getValue();
            } else if (value instanceof Resource) {
                return ((Resource) value).getIdElement().toVersionless().getValue();
            } else if (value != null) {
                return value.toString();
            } else {
                return null;
            }
        }

        public Object getValue() {
            return this.value;
        }

        public Class<?> getValueClass() {
            if (this.value == null) {
                return String.class;
            }

            return this.value.getClass();
        }

        private String joinValues(String... elements) {
            return String.join("-", elements);
        }
    }
}
