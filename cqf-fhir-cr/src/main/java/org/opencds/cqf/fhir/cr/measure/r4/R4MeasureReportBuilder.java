package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.FhirResourceUtils;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureInfo;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R4MeasureReportBuilder implements MeasureReportBuilder<Measure, MeasureReport, DomainResource> {

    private static final Logger logger = LoggerFactory.getLogger(R4MeasureReportBuilder.class);
    protected static final String POPULATION_SUBJECT_SET = "POPULATION_SUBJECT_SET";

    @Override
    public MeasureReport build(
            Measure measure,
            MeasureDef measureDef,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds) {

        var report = this.createMeasureReport(measure, measureDef, measureReportType, subjectIds, measurementPeriod);

        var bc = new R4MeasureReportBuilderContext(measure, measureDef, report);

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
        var measure = bc.measure();
        var measureDef = bc.measureDef();
        var report = bc.report();

        if (measure.getGroup().size() != measureDef.groups().size()) {
            throw new InternalErrorException(
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

    private void buildGroup(
            R4MeasureReportBuilderContext bc,
            MeasureGroupComponent measureGroup,
            MeasureReportGroupComponent reportGroup,
            GroupDef groupDef) {

        var groupDefSizeDiff = 0;
        if (groupDef.hasPopulationType(MeasurePopulationType.DATEOFCOMPLIANCE)) {
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
        R4MeasureReportUtils.addExtensionImprovementNotation(reportGroup, groupDef);

        for (int i = 0; i < measureGroup.getPopulation().size(); i++) {
            // Report Population Component
            var measurePop = measureGroup.getPopulation().get(i);
            // Groups can have more than one of the same PopulationType, we need a Unique value to bind on
            PopulationDef defPop = groupDef.findPopulationById(measurePop.getId());
            var reportPop = reportGroup.addPopulation();
            buildPopulation(bc, measurePop, reportPop, defPop, groupDef);
        }

        // add extension to group for totalDenominator and totalNumerator
        if (groupDef.measureScoring().equals(MeasureScoring.PROPORTION)
                || groupDef.measureScoring().equals(MeasureScoring.RATIO)
                || groupDef.measureScoring().equals(MeasureScoring.CONTINUOUSVARIABLE)) {

            // add extension to group for
            if (bc.report().getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)) {
                var docPopDef = groupDef.findPopulationByType(DATEOFCOMPLIANCE);
                if (docPopDef != null
                        && docPopDef.getAllSubjectResources() != null
                        && !docPopDef.getAllSubjectResources().isEmpty()) {
                    var docValue = docPopDef.getAllSubjectResources().iterator().next();
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
            R4StratifierBuilder.buildStratifier(
                    bc, groupStrat, reportStrat, defStrat, measureGroup.getPopulation(), groupDef);
        }
    }

    private void addMeasureDescription(MeasureReportGroupComponent reportGroup, MeasureGroupComponent measureGroup) {
        if (measureGroup.hasDescription()) {
            reportGroup.addExtension(
                    MeasureConstants.EXT_POPULATION_DESCRIPTION_URL, new StringType(measureGroup.getDescription()));
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
            MeasureGroupPopulationComponent measurePopulation,
            MeasureReportGroupPopulationComponent reportPopulation,
            PopulationDef populationDef,
            GroupDef groupDef) {

        reportPopulation.setCode(measurePopulation.getCode());
        reportPopulation.setId(measurePopulation.getId());
        reportPopulation.setCount(populationDef.getCount());

        // Supporting Evidence
        if (bc.report().getType().equals(MeasureReport.MeasureReportType.INDIVIDUAL)
                && populationDef.getSupportingEvidenceDefs() != null
                && !populationDef.getSupportingEvidenceDefs().isEmpty()) {
            var extDefs = populationDef.getSupportingEvidenceDefs();
            R4SupportingEvidenceExtension.addSupportingEvidenceExtensions(reportPopulation, extDefs);
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
                    .map(FhirResourceUtils::addPatientQualifier)
                    .collect(Collectors.toSet());
        } else {
            populationSet = populationDef.getAllSubjectResources().stream()
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
        if (sde.getResults().isEmpty()) {
            return;
        }

        // Add all evaluated resources
        for (var e : sde.getResults().entrySet()) {
            addEvaluatedResourceReferences(bc, sde.id(), e.getValue().evaluatedResources());
        }

        CodeableConcept concept = conceptDefToConcept(sde.code());

        Map<StratumValueWrapper, Long> accumulated = sde.getResults().values().stream()
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

                bc.addCriteriaExtensionToSupplementalData(obs, sde.id());
            } else {
                bc.addCriteriaExtensionToSupplementalData(resource, sde.id());
            }
        }
    }

    private void buildSDEs(R4MeasureReportBuilderContext bc) {
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

    private MeasureReport createMeasureReport(
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

    private Extension createMeasureInfoExtension(MeasureInfo measureInfo) {

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
            Double groupScore = groupDef.getScore();
            if (groupScore != null) {
                reportGroup.getMeasureScore().setValue(groupScore);
            }

            copyPopulationAggregationResults(reportGroup, groupDef);

            // Copy stratifier scores
            copyStratifierScores(reportGroup, groupDef);
        }
    }

    private void copyPopulationAggregationResults(MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
        for (MeasureReportGroupPopulationComponent reportPopulation : reportGroup.getPopulation()) {
            var populationDef = groupDef.findPopulationById(reportPopulation.getId());
            R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(reportPopulation, populationDef);
        }
    }

    /**
     * Copy stratifier scores from StratifierDef objects to MeasureReport stratifiers.
     * Logic is driven by StratifierDef objects, matching report stratifiers by ID.
     *
     * @param reportGroup the MeasureReport group component
     * @param groupDef the GroupDef containing stratifier scores
     */
    private void copyStratifierScores(MeasureReportGroupComponent reportGroup, GroupDef groupDef) {
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
            for (var stratumDef : stratifierDef.getStratum()) {
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
            }
        }
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
            MeasureReport.StratifierGroupComponent reportStratum, StratumDef stratumDef, StratifierDef stratifierDef) {
        return R4MeasureReportUtils.matchesStratumValue(reportStratum, stratumDef, stratifierDef);
    }
}
