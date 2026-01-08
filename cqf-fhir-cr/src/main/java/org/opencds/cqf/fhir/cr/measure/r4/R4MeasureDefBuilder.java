package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_USAGE_CODE;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponentComponent;
import org.hl7.fhir.r4.model.Measure.MeasureSupplementalDataComponent;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefBuilder;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.SdeDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierComponentDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureUtils;

public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // scoring
        var measureScoring = R4MeasureUtils.getMeasureScoring(measure);
        // populationBasis
        var measureBasis = getMeasureBasis(measure);
        // improvement Notation
        var measureImpNotation = getMeasureImprovementNotation(measure);

        // Groups
        List<GroupDef> groups = new ArrayList<>();
        for (MeasureGroupComponent group : measure.getGroup()) {
            var groupDef = buildGroupDef(measure, group, measureScoring, measureImpNotation, measureBasis);

            groups.add(groupDef);
        }

        return new MeasureDef(
                // We don't need either the version of the "Measure" qualifier here
                measure.getIdElement(), measure.getUrl(), measure.getVersion(), groups, getSdeDefs(measure));
    }

    private GroupDef buildGroupDef(
            Measure measure,
            MeasureGroupComponent group,
            MeasureScoring measureScoring,
            CodeDef measureImpNotation,
            CodeDef measureBasis) {

        // group Measure Scoring
        var groupScoring = getGroupMeasureScoring(measure, group);
        // populationBasis
        var groupBasis = getGroupPopulationBasis(group);
        // improvement Notation
        var groupImpNotation = getGroupImpNotation(measure, group);
        var hasGroupImpNotation = groupImpNotation != null;

        // Populations
        checkIds(group);

        var populationBasisDef = getPopulationBasisDef(measureBasis, groupBasis);
        var populationsWithCriteriaReference = group.getPopulation().stream()
                .map(t -> buildPopulationDef(t, group, measure.getUrl(), populationBasisDef))
                .toList();

        final Optional<PopulationDef> optPopulationDefDateOfCompliance = buildPopulationDefForDateOfCompliance(
                measure.getUrl(), group, populationsWithCriteriaReference, populationBasisDef);

        // Stratifiers
        var stratifiers = group.getStratifier().stream()
                .map(mgsc -> buildStratifierDef(measure.getUrl(), mgsc))
                .toList();

        return new GroupDef(
                group.getId(),
                conceptToConceptDef(group.getCode()),
                stratifiers,
                mergePopulations(populationsWithCriteriaReference, optPopulationDefDateOfCompliance.orElse(null)),
                R4MeasureUtils.computeScoring(measure.getUrl(), measureScoring, groupScoring),
                hasGroupImpNotation,
                getImprovementNotation(measureImpNotation, groupImpNotation),
                populationBasisDef);
    }

    private void checkIds(MeasureGroupComponent group) {
        group.getPopulation().forEach(R4MeasureDefBuilder::checkId);
    }

    @Nonnull
    private PopulationDef buildPopulationDef(
            MeasureGroupPopulationComponent population,
            MeasureGroupComponent group,
            String measureUrl,
            CodeDef populationBasis) {
        MeasurePopulationType popType = MeasurePopulationType.fromCode(
                population.getCode().getCodingFirstRep().getCode());
        // criteriaReference & aggregateMethod are for MeasureObservation populations only
        String criteriaReference = getCriteriaReference(group, population, popType, measureUrl);
        ContinuousVariableObservationAggregateMethod aggregateMethod =
                R4MeasureUtils.getAggregateMethod(measureUrl, population);
        return new PopulationDef(
                population.getId(),
                conceptToConceptDef(population.getCode()),
                popType,
                population.getCriteria().getExpression(),
                populationBasis,
                criteriaReference,
                aggregateMethod);
    }

    private Optional<PopulationDef> buildPopulationDefForDateOfCompliance(
            String measureUrl,
            MeasureGroupComponent group,
            List<PopulationDef> populationDefs,
            CodeDef populationBasis) {

        if (group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL) == null
                || checkPopulationForCode(populationDefs, DATEOFCOMPLIANCE) == null) {
            return Optional.empty();
        }

        // add to definition
        var expressionType = (Expression) group.getExtensionByUrl(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL)
                .getValue();
        if (!expressionType.hasExpression()) {
            throw new InvalidRequestException("no expression was listed for extension: %s for Measure: %s"
                    .formatted(CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL, measureUrl));
        }
        var expression = expressionType.getExpression();
        var populateDefDateOfCompliance = new PopulationDef(
                "dateOfCompliance",
                totalConceptDefCreator(DATEOFCOMPLIANCE),
                DATEOFCOMPLIANCE,
                expression,
                populationBasis);

        return Optional.of(populateDefDateOfCompliance);
    }

    @Nullable
    private String getCriteriaReference(
            MeasureGroupComponent group,
            @Nullable MeasureGroupPopulationComponent population,
            MeasurePopulationType measurePopulationType,
            String measureUrl) {

        if (!measurePopulationType.equals(MeasurePopulationType.MEASUREOBSERVATION)) {
            return null;
        }

        if (population == null) {
            throw new InvalidRequestException("group.population is null");
        }
        var populationCriteriaExt = population.getExtensionByUrl(EXT_CQFM_CRITERIA_REFERENCE);
        if (populationCriteriaExt != null) {
            // required for measure-observation populations
            // the underlying expression is a cql function
            // the criteria reference is what is used to populate parameters of the function
            String critReference = populationCriteriaExt.getValue().toString();
            // check that the reference exists in the GroupDef.populationId
            if (group.getPopulation().stream().map(Element::getId).noneMatch(id -> id.equals(critReference))) {
                throw new InvalidRequestException(
                        "no matching criteria reference was found for extension: %s for Measure: %s"
                                .formatted(EXT_CQFM_CRITERIA_REFERENCE, measureUrl));
            }
            // assign validated reference
            return critReference;
        }

        return null;
    }

    @Nonnull
    private StratifierDef buildStratifierDef(String measureUrl, MeasureGroupStratifierComponent mgsc) {
        checkId(mgsc);

        // Components
        var components = new ArrayList<StratifierComponentDef>();
        for (MeasureGroupStratifierComponentComponent scc : mgsc.getComponent()) {
            checkId(scc);
            var scd = new StratifierComponentDef(
                    scc.getId(),
                    conceptToConceptDef(scc.getCode()),
                    scc.hasCriteria() ? scc.getCriteria().getExpression() : null);

            components.add(scd);
        }

        if (!components.isEmpty() && mgsc.getCriteria().getExpression() != null) {
            throw new InvalidRequestException(
                    "Measure: %s with stratifier: %s, has both components and stratifier criteria expressions defined. Only one should be specified"
                            .formatted(measureUrl, mgsc.getId()));
        }

        return new StratifierDef(
                mgsc.getId(),
                conceptToConceptDef(mgsc.getCode()),
                mgsc.getCriteria().getExpression(),
                getStratifierType(measureUrl, mgsc),
                components);
    }

    public static void triggerFirstPassValidation(List<Measure> measures) {
        measures.forEach(R4MeasureDefBuilder::triggerFirstPassValidation);
    }

    @Nonnull
    private List<SdeDef> getSdeDefs(Measure measure) {
        final List<SdeDef> sdes = new ArrayList<>();
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(measure, s);
            var sdeDef = new SdeDef(
                    s.getId(), conceptToConceptDef(s.getCode()), s.getCriteria().getExpression());
            sdes.add(sdeDef);
        }
        return sdes;
    }

    private static MeasureStratifierType getStratifierType(
            String measureUrl, MeasureGroupStratifierComponent measureGroupStratifierComponent) {
        if (measureGroupStratifierComponent == null) {
            return MeasureStratifierType.VALUE;
        }

        final boolean hasCriteria = measureGroupStratifierComponent.hasCriteria();

        final boolean hasAnyComponentCriteria = measureGroupStratifierComponent.getComponent().stream()
                .anyMatch(MeasureGroupStratifierComponentComponent::hasCriteria);

        if (hasCriteria && hasAnyComponentCriteria) {
            throw new InvalidRequestException(
                    "Stratifier Cannot have both criteria: %s and any component criteria: %s for measure: %s"
                            .formatted(hasCriteria, hasAnyComponentCriteria, measureUrl));
        }

        if (!hasCriteria && !hasAnyComponentCriteria) {
            throw new InvalidRequestException(
                    "Stratifier cannot have neither criteria nor component for measure: %s".formatted(measureUrl));
        }

        if (hasCriteria) {
            return MeasureStratifierType.CRITERIA;
        }

        return MeasureStratifierType.VALUE;
    }

    private static void triggerFirstPassValidation(Measure measure) {

        checkId(measure);

        // SDES
        for (MeasureSupplementalDataComponent s : measure.getSupplementalData()) {
            checkId(s);
            checkSDEUsage(measure, s);
        }

        // Create instance to call instance methods
        var builder = new R4MeasureDefBuilder();

        builder.validateMeasureImprovementNotation(measure);
    }

    private static void checkSDEUsage(
            Measure measure, MeasureSupplementalDataComponent measureSupplementalDataComponent) {
        var hasUsage = measureSupplementalDataComponent.getUsage().stream()
                .filter(t -> t.getCodingFirstRep().getCode().equals(SDE_USAGE_CODE))
                .toList();
        if (CollectionUtils.isEmpty(hasUsage)) {
            throw new InvalidRequestException("SupplementalDataComponent usage is missing code: %s for Measure: %s"
                    .formatted(SDE_USAGE_CODE, measure.getUrl()));
        }
    }

    private ConceptDef conceptToConceptDef(CodeableConcept codeable) {
        if (codeable == null) {
            return null;
        }

        List<CodeDef> codes = new ArrayList<>();
        for (var c : codeable.getCoding()) {
            codes.add(codeToCodeDef(c));
        }

        return new ConceptDef(codes, codeable.getText());
    }

    private CodeDef codeToCodeDef(Coding coding) {
        return new CodeDef(coding.getSystem(), coding.getVersion(), coding.getCode(), coding.getDisplay());
    }

    private static void checkId(Element e) {
        if (e.getId() == null || StringUtils.isBlank(e.getId())) {
            throw new InvalidRequestException("id is required on all Elements of type: " + e.fhirType());
        }
    }

    private static void checkId(Resource r) {
        if (r.getId() == null || StringUtils.isBlank(r.getId())) {
            throw new InvalidRequestException("id is required on all Resources of type: " + r.fhirType());
        }
    }

    public CodeDef getMeasureBasis(Measure measure) {

        var ext = measure.getExtensionByUrl(MeasureConstants.POPULATION_BASIS_URL);
        // check for population-basis Extension, assume boolean if no Extension is found
        if (ext != null) {
            return makeCodeDefFromExtension(ext);
        }
        return null;
    }

    private CodeDef makeCodeDefFromExtension(Extension extension) {
        var code = extension.getValue().toString();
        // validate code membership
        assert Enumerations.FHIRAllTypes.fromCode(code) != null;
        return new CodeDef(MeasureConstants.POPULATION_BASIS_URL, code);
    }

    public CodeDef getMeasureImprovementNotation(Measure measure) {
        if (measure.hasImprovementNotation()) {
            var improvementNotationValue = measure.getImprovementNotation();
            var codeDef = new CodeDef(
                    improvementNotationValue.getCodingFirstRep().getSystem(),
                    improvementNotationValue.getCodingFirstRep().getCode());
            R4MeasureUtils.validateImprovementNotationCode(measure.getUrl(), codeDef);
            return codeDef;
        }
        return null;
    }

    private void validateMeasureImprovementNotation(Measure measure) {
        if (measure.hasImprovementNotation()) {
            var improvementNotationValue = measure.getImprovementNotation();
            var codeDef = new CodeDef(
                    improvementNotationValue.getCodingFirstRep().getSystem(),
                    improvementNotationValue.getCodingFirstRep().getCode());
            R4MeasureUtils.validateImprovementNotationCode(measure.getUrl(), codeDef);
        }
    }

    public CodeDef getGroupImpNotation(Measure measure, MeasureGroupComponent group) {
        return R4MeasureUtils.getGroupImprovementNotation(measure, group);
    }

    public MeasureScoring getGroupMeasureScoring(Measure measure, MeasureGroupComponent group) {
        return R4MeasureUtils.getGroupMeasureScoring(measure, group);
    }

    public CodeDef getGroupPopulationBasis(MeasureGroupComponent group) {
        var ext = group.getExtensionByUrl(MeasureConstants.POPULATION_BASIS_URL);
        // check for population-basis Extension, assume boolean if no Extension is found
        if (ext != null) {
            return makeCodeDefFromExtension(ext);
        }
        return null;
    }
}
