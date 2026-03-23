package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.FhirResourceUtils;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;
import org.opencds.cqf.fhir.cr.measure.common.MeasureInfo;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4ConceptDefs;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4MeasureReportBuilder implements MeasureReportBuilder<MeasureReport> {

    private static final Logger logger = LoggerFactory.getLogger(R4MeasureReportBuilder.class);

    @Override
    public MeasureReport build(
            MeasureDef measureDef,
            MeasureEvaluationState state,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds) {

        var report = this.createMeasureReport(measureDef, measureReportType, subjectIds, measurementPeriod);

        var bc = new R4MeasureReportBuilderContext(measureDef, state, report);

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

        // Copy scores from Def objects (populated by MeasureReportDefScorer in MeasureEvaluationResultHandler)
        copyScoresFromDef(bc);

        setReportStatus(bc);
        return bc.report();
    }

    private void setReportStatus(R4MeasureReportBuilderContext bc) {
        if (bc.report().hasContained()
                && bc.report().getContained().stream()
                        .anyMatch(t -> t.getResourceType().equals(ResourceType.OperationOutcome))) {
            // Measure Reports that have encountered an error during evaluation will be set to status 'Error'
            bc.report().setStatus(MeasureReportStatus.ERROR);
        }
    }

    private void addSupplementalData(R4MeasureReportBuilderContext bc) {
        var report = bc.report();

        for (Reference r : bc.supplementalDataReferences().values()) {
            report.addExtension(EXT_SDE_REFERENCE_URL, r);
        }
    }

    private void addEvaluatedResource(R4MeasureReportBuilderContext bc) {
        var report = bc.report();
        // Only add evaluated resources to individual reports
        if (report.getType() == org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.INDIVIDUAL) {
            for (Reference r : bc.evaluatedResourceReferences().values()) {
                report.addEvaluatedResource(r);
            }
        }
    }

    private void buildGroups(R4MeasureReportBuilderContext bc) {
        var measureDef = bc.measureDef();
        var report = bc.report();

        for (var defGroup : measureDef.groups()) {
            var reportGroup = report.addGroup();
            buildGroup(bc, reportGroup, defGroup);
        }
    }

    private void buildGroup(
            R4MeasureReportBuilderContext bc, MeasureReportGroupComponent reportGroup, GroupDef groupDef) {

        reportGroup.setCode(R4ConceptDefs.toConcept(groupDef.code()));
        reportGroup.setId(groupDef.id());
        // Measure Level Extension
        addDescriptionExtension(reportGroup, groupDef.description());
        R4MeasureReportUtils.addExtensionImprovementNotation(reportGroup, groupDef);

        for (PopulationDef defPop : groupDef.populations()) {
            if (defPop.type() == MeasurePopulationType.DATEOFCOMPLIANCE) continue;
            var reportPop = reportGroup.addPopulation();
            buildPopulation(bc, reportPop, defPop, groupDef);
        }

        // add extension to group for totalDenominator and totalNumerator
        if (groupDef.measureScoring().equals(MeasureScoring.PROPORTION)
                || groupDef.measureScoring().equals(MeasureScoring.RATIO)
                || groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE)) {

            // add extension to group for
            if (bc.report().getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)) {
                var docPopDef = groupDef.findPopulationByType(DATEOFCOMPLIANCE);
                if (docPopDef != null) {
                    var docPopState = bc.state().population(docPopDef);
                    if (docPopState.getAllSubjectResources() != null
                            && !docPopState.getAllSubjectResources().isEmpty()) {
                        var docValue =
                                docPopState.getAllSubjectResources().iterator().next();
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
        }
        for (var defStrat : groupDef.stratifiers()) {
            var reportStrat = reportGroup.addStratifier();
            R4StratifierBuilder.buildStratifier(bc, reportStrat, defStrat, groupDef);
        }
    }

    private String getPopulationResourceIds(Object resourceObject) {
        if (resourceObject instanceof IBaseResource resource) {
            return resource.getIdElement().toVersionless().getValueAsString();
        }
        return null;
    }

    private void buildPopulation(
            R4MeasureReportBuilderContext bc,
            MeasureReportGroupPopulationComponent reportPopulation,
            PopulationDef populationDef,
            GroupDef groupDef) {

        reportPopulation.setCode(R4ConceptDefs.toConcept(populationDef.code()));
        reportPopulation.setId(populationDef.id());
        reportPopulation.setCount(bc.state().population(populationDef).getCount());

        // Supporting Evidence
        if (bc.report().getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)
                && populationDef.getSupportingEvidenceDefs() != null
                && !populationDef.getSupportingEvidenceDefs().isEmpty()) {
            var extDefs = populationDef.getSupportingEvidenceDefs();
            R4SupportingEvidenceExtension.addSupportingEvidenceExtensions(reportPopulation, extDefs, bc.state());
        }

        addDescriptionExtension(reportPopulation, populationDef.description());

        addEvaluatedResourceReferences(
                bc, populationDef.id(), bc.state().population(populationDef).getEvaluatedResources());

        // This is a temporary list carried forward to stratifiers
        // subjectResult set defined by basis of Measure
        Set<String> populationSet;
        if (groupDef.isBooleanBasis()) {
            populationSet = bc.state().population(populationDef).getSubjects().stream()
                    .map(FhirResourceUtils::addPatientQualifier)
                    .collect(Collectors.toSet());
        } else {
            populationSet = bc.state().population(populationDef).getAllSubjectResources().stream()
                    .filter(Resource.class::isInstance)
                    .map(this::getPopulationResourceIds)
                    .collect(Collectors.toSet());
        }

        // Report Type behavior
        if (Objects.requireNonNull(bc.report().getType()) == MeasureReport.MeasureReportType.SUBJECTLIST
                && !populationSet.isEmpty()) {
            ListResource subjectList = createIdList(UUID.randomUUID().toString(), populationSet);
            bc.addContained(subjectList);
            reportPopulation.setSubjectResults(new Reference("#" + subjectList.getId()));
        }
    }

    static ListResource createList(String id) {
        return (ListResource) new ListResource().setId(id);
    }

    private ListResource createIdList(String id, Collection<String> ids) {
        return this.createReferenceList(id, ids.stream().map(Reference::new).collect(Collectors.toList()));
    }

    private ListResource createReferenceList(String id, Collection<Reference> references) {
        ListResource referenceList = createList(id);
        for (Reference reference : references) {
            referenceList.addEntry().setItem(reference);
        }

        return referenceList;
    }

    private void addEvaluatedResourceReferences(
            R4MeasureReportBuilderContext bc, String criteriaId, Set<Object> evaluatedResources) {
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
    private void buildSDE(R4MeasureReportBuilderContext bc, SdeDef sde) {
        var report = bc.report();

        // No SDEs were calculated, do nothing
        var sdeResults = bc.state().sde(sde).getResults();
        if (sdeResults.isEmpty()) {
            return;
        }

        // Add all evaluated resources
        for (var e : sdeResults.entrySet()) {
            addEvaluatedResourceReferences(bc, sde.id(), e.getValue().evaluatedResources());
        }

        CodeableConcept concept = R4ConceptDefs.toConcept(sde.code());

        Map<StratumValueWrapper, Long> accumulated = sdeResults.values().stream()
                .flatMap(x -> Lists.newArrayList(x.iterableValue()).stream())
                .filter(Objects::nonNull)
                .map(StratumValueWrapper::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        for (Map.Entry<StratumValueWrapper, Long> accumulator : accumulated.entrySet()) {

            Resource obs;
            if (!(accumulator.getKey().getValue() instanceof Resource resource)) {
                String valueCode = accumulator.getKey().getValueAsString();
                Long valueCount = accumulator.getValue();

                Coding valueCoding = new Coding().setCode(valueCode);

                if (Objects.requireNonNull(report.getType()) == MeasureReport.MeasureReportType.INDIVIDUAL) {
                    obs = createPatientObservation(bc, UUID.randomUUID().toString(), sde.id(), valueCoding, concept);
                } else {
                    obs = createPopulationObservation(
                            bc, UUID.randomUUID().toString(), sde.id(), valueCoding, valueCount, concept);
                }

                bc.addCriteriaExtensionToSupplementalData(obs, sde.id(), sde.description());
            } else {
                bc.addCriteriaExtensionToSupplementalData(resource, sde.id(), sde.description());
            }
        }
    }

    private void buildSDEs(R4MeasureReportBuilderContext bc) {
        for (var sde : bc.measureDef().sdes()) {
            buildSDE(bc, sde);
        }
    }

    private MeasureReport createMeasureReport(
            MeasureDef measureDef, MeasureReportType type, List<String> subjectIds, Interval measurementPeriod) {
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

        report.setMeasure(getMeasureReference(measureDef));
        report.setDate(new java.util.Date());
        report.setImplicitRules(measureDef.implicitRules());
        if (measureDef.groups().isEmpty() || !measureDef.groups().get(0).isGroupImprovementNotation()) {
            // Use the measure-level improvement notation if explicitly set (null means not set)
            if (measureDef.measureImprovementNotation() != null) {
                report.setImprovementNotation(improvementNotationToConcept(measureDef.measureImprovementNotation()));
            }
        }
        report.setLanguage(measureDef.language());

        if (measureDef.description() != null && !measureDef.description().isEmpty()) {
            report.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measureDef.description()));
        }

        return report;
    }

    private Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

        Extension extExtMeasure =
                new Extension().setUrl(MeasureInfo.MEASURE).setValue(new CanonicalType(measureInfo.getMeasure()));

        Extension obsExtension = new Extension().setUrl(MeasureInfo.EXT_URL);
        obsExtension.addExtension(extExtMeasure);

        return obsExtension;
    }

    private String getMeasureReference(MeasureDef measureDef) {
        if (StringUtils.isNotBlank(measureDef.url())
                && !measureDef.url().contains("|")
                && StringUtils.isNotBlank(measureDef.version())) {
            return measureDef.url() + "|" + measureDef.version();
        }
        return measureDef.url();
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

    private DomainResource createPopulationObservation(
            R4MeasureReportBuilderContext bc,
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

    private DomainResource createPatientObservation(
            R4MeasureReportBuilderContext bc,
            String id,
            String populationId,
            Coding valueCoding,
            CodeableConcept originalConcept) {

        Observation obs = createObservation(bc, id, populationId);

        obs.setCode(getMeasureUsageConcept(originalConcept));

        CodeableConcept valueCodeableConcept = new CodeableConcept();
        valueCodeableConcept.setCoding(Collections.singletonList(valueCoding));
        obs.setValue(valueCodeableConcept);
        return obs;
    }

    private Observation createObservation(R4MeasureReportBuilderContext bc, String id, String populationId) {
        var measureUrl = bc.measureDef().url();
        var measureId = bc.measureDef().id();
        MeasureInfo measureInfo = new MeasureInfo()
                .withMeasure(
                        StringUtils.isNotBlank(measureUrl)
                                ? measureUrl
                                : (StringUtils.isNotBlank(measureId) ? MeasureInfo.MEASURE_PREFIX + measureId : ""))
                .withPopulationId(populationId);

        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        obs.setId(id);
        obs.addExtension(createMeasureInfoExtension(measureInfo));

        return obs;
    }

    private Observation createMeasureObservation(R4MeasureReportBuilderContext bc, String id, String observationName) {
        Observation obs = this.createObservation(bc, id, observationName);
        CodeableConcept cc = new CodeableConcept();
        cc.setText(observationName);
        obs.setCode(cc);
        return obs;
    }

    /**
     * Copy scores from MeasureDef to MeasureReport.
     *
     * <p>Logic is driven by Def objects, matching report structures by ID.
     * Logs warnings when matching report structures are not found.
     *
     * @param bc the builder context
     */
    private void copyScoresFromDef(R4MeasureReportBuilderContext bc) {
        var report = bc.report();
        var measureDef = bc.measureDef();

        // Iterate through GroupDefs (drive from Def side)
        for (var groupDef : measureDef.groups()) {
            MeasureReportGroupComponent reportGroup;

            // For single-group measures, use positional matching (no ID required)
            // For multi-group measures, match by ID
            if (report.getGroup().size() == 1) {
                reportGroup = report.getGroupFirstRep();
            } else {
                // Multi-group: match by ID
                reportGroup = report.getGroup().stream()
                        .filter(rg -> groupDef.id() != null && groupDef.id().equals(rg.getId()))
                        .findFirst()
                        .orElse(null);
            }

            if (reportGroup == null) {
                logger.warn("No matching MeasureReport group found for GroupDef with id: {}", groupDef.id());
                continue;
            }

            // Copy group-level score
            Double groupScore = bc.state().group(groupDef).getScore();
            if (groupScore != null) {
                reportGroup.getMeasureScore().setValue(groupScore);
            }

            copyPopulationAggregationResults(bc, reportGroup, groupDef);

            // Copy stratifier scores
            copyStratifierScores(bc, reportGroup, groupDef);
        }
    }

    private void copyPopulationAggregationResults(
            R4MeasureReportBuilderContext bc, MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        for (MeasureReportGroupPopulationComponent reportPopulation : reportGroup.getPopulation()) {
            var populationDef = groupDef.findPopulationById(reportPopulation.getId());
            R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                    reportPopulation,
                    populationDef.getAggregateMethod(),
                    bc.state().population(populationDef).getAggregationResult(),
                    populationDef.getCriteriaReference());
        }
    }

    /**
     * Copy stratifier scores from StratifierDef objects to MeasureReport stratifiers.
     * Logic is driven by StratifierDef objects, matching report stratifiers by ID.
     *
     * @param reportGroup the MeasureReport group component
     * @param groupDef the GroupDef containing stratifier scores
     */
    private void copyStratifierScores(
            R4MeasureReportBuilderContext bc, MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        // Iterate through StratifierDefs (drive from Def side)
        for (var stratifierDef : groupDef.stratifiers()) {
            // Find matching report stratifier by ID
            var reportStratifier = reportGroup.getStratifier().stream()
                    .filter(rs -> stratifierDef.id().equals(rs.getId()))
                    .findFirst()
                    .orElse(null);

            if (reportStratifier == null) {
                logger.warn(
                        "No matching MeasureReport stratifier found for StratifierDef with id: {}", stratifierDef.id());
                continue;
            }

            // Iterate through StratumDefs (drive from Def side)
            for (var stratumDef : bc.state().stratifier(stratifierDef).getStrata()) {
                // Find matching report stratum by comparing value strings
                var reportStratum = reportStratifier.getStratum().stream()
                        .filter(rs -> matchesStratumValue(rs, stratumDef, stratifierDef))
                        .findFirst()
                        .orElse(null);

                if (reportStratum == null) {
                    logger.debug(
                            "No matching MeasureReport stratum found for StratumDef in stratifier: {}",
                            stratifierDef.id());
                    continue;
                }

                // Copy stratum score
                Double stratumScore = stratumDef.getScore();
                if (stratumScore != null) {
                    reportStratum.getMeasureScore().setValue(stratumScore);
                }

                // Copy per-stratum population aggregation results
                copyStratumPopulationAggregationResults(reportStratum, stratumDef);
            }
        }
    }

    /**
     * Copy per-stratum aggregation results to stratum population extensions.
     * This persists the intermediate observation aggregates (numerator/denominator)
     * that are needed for downstream distributed aggregation.
     */
    private void copyStratumPopulationAggregationResults(
            StratifierGroupComponent reportStratum, StratumDef stratumDef) {

        for (StratifierGroupPopulationComponent reportStratumPopulation : reportStratum.getPopulation()) {
            copySingleStratumPopulationAggregationResult(
                    reportStratumPopulation, stratumDef.findPopulationById(reportStratumPopulation.getId()));
        }
    }

    private void copySingleStratumPopulationAggregationResult(
            StratifierGroupPopulationComponent reportStratumPopulation, StratumPopulationDef stratumPopulationDef) {

        Double aggregationResult = stratumPopulationDef.getAggregationResult();

        PopulationDef populationDef = stratumPopulationDef.populationDef();
        if (populationDef == null) {
            return;
        }

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                reportStratumPopulation,
                populationDef.getAggregateMethod(),
                aggregationResult,
                populationDef.getCriteriaReference());
    }

    /**
     * Check if a MeasureReport stratum matches a StratumDef by comparing text representations.
     * Delegates to R4MeasureReportUtils for text-based comparison logic.
     *
     * @param reportStratum the MeasureReport stratum
     * @param stratumDef the StratumDef
     * @param stratifierDef the parent StratifierDef (for context)
     * @return true if values match
     */
    private boolean matchesStratumValue(
            StratifierGroupComponent reportStratum, StratumDef stratumDef, StratifierDef stratifierDef) {
        return R4MeasureReportUtils.matchesStratumValue(reportStratum, stratumDef, stratifierDef);
    }

    private void addDescriptionExtension(Element target, String description) {
        if (description != null && !description.isEmpty()) {
            target.addExtension(MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(description));
        }
    }

    private CodeableConcept improvementNotationToConcept(CodeDef impNot) {
        if (impNot == null) return null;
        return new CodeableConcept().addCoding(R4ConceptDefs.toCoding(impNot));
    }
}
