package org.opencds.cqf.fhir.cr.measure.r4;

import static org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType.DATEOFCOMPLIANCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQF_EXPRESSION_CODE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SUPPORTING_EVIDENCE_DEFINITION_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.SDE_USAGE_CODE;
import static org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureUtils.isBooleanPopulationBasis;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureUtils;

@SuppressWarnings("squid:S1135")
public class R4MeasureDefBuilder implements MeasureDefBuilder<Measure> {

    @Override
    public MeasureDef build(Measure measure) {
        checkId(measure);

        // scoring
        @Nullable final MeasureScoring measureScoring = R4MeasureUtils.getMeasureScoring(measure);
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
                measure.getIdElement(),
                measure.getUrl(),
                measure.getVersion(),
                measureScoring,
                groups,
                getSdeDefs(measure));
    }

    private GroupDef buildGroupDef(
            Measure measure,
            MeasureGroupComponent group,
            @Nullable MeasureScoring measureScoring,
            CodeDef measureImpNotation,
            CodeDef measureBasis) {

        // group Measure Scoring
        var groupScoring = getGroupMeasureScoring(measure, group);

        if (measureScoring != null && groupScoring != null) {
            throw new InvalidRequestException(
                    "Scoring should be at the measure level or the group level, but not both for measure: %s"
                            .formatted(measure.getUrl()));
        }

        // populationBasis
        var groupBasis = getGroupPopulationBasis(group);
        // improvement Notation
        var groupImpNotation = getGroupImpNotation(measure, group);
        var hasGroupImpNotation = groupImpNotation != null;

        // Populations
        checkIds(group);
        validateRatioContinuousVariableIfApplicable(measure, group, groupScoring, measureScoring);

        var populationBasisDef = getPopulationBasisDef(measureBasis, groupBasis);
        var populationsWithCriteriaReference = group.getPopulation().stream()
                .map(t -> buildPopulationDef(
                        t, group, measure.getUrl(), populationBasisDef, getSupportingEvidenceDefs(t)))
                .toList();

        final Optional<PopulationDef> optPopulationDefDateOfCompliance = buildPopulationDefForDateOfCompliance(
                measure.getUrl(), group, populationsWithCriteriaReference, populationBasisDef);

        // Stratifiers
        var stratifiers = group.getStratifier().stream()
                .map(mgsc -> buildStratifierDef(measure.getUrl(), mgsc, populationBasisDef))
                .toList();

        return new GroupDef(
                group.getId(),
                conceptToConceptDef(group.getCode()),
                stratifiers,
                mergePopulations(populationsWithCriteriaReference, optPopulationDefDateOfCompliance.orElse(null)),
                groupScoring,
                hasGroupImpNotation,
                getImprovementNotation(measureImpNotation, groupImpNotation),
                populationBasisDef);
    }

    private List<SupportingEvidenceDef> getSupportingEvidenceDefs(MeasureGroupPopulationComponent groupPopulation) {

        List<SupportingEvidenceDef> supportingEvidenceDefs = new ArrayList<>();

        List<Extension> ext = groupPopulation.getExtension().stream()
                .filter(t -> EXT_SUPPORTING_EVIDENCE_DEFINITION_URL.equals(t.getUrl()))
                .toList();

        for (Extension e : ext) {

            if (!(e.getValue() instanceof Expression expressionValue)) {
                throw new InvalidRequestException("Extension does not contain valueExpression");
            }

            supportingEvidenceDefs.add(new SupportingEvidenceDef(
                    expressionValue.getExpression(),
                    EXT_SUPPORTING_EVIDENCE_URL,
                    expressionValue.getDescription(),
                    expressionValue.getName(),
                    expressionValue.getLanguage(),
                    extractConceptDefFromExpression(expressionValue)));
        }

        return supportingEvidenceDefs;
    }

    public static ConceptDef extractConceptDefFromExpression(Expression expr) {
        if (expr == null) {
            return null;
        }

        Coding coding = expr.getExtension().stream()
                .filter(e -> EXT_CQF_EXPRESSION_CODE.equals(e.getUrl()))
                .map(Extension::getValue)
                .filter(Coding.class::isInstance)
                .map(Coding.class::cast)
                .findFirst()
                .orElse(null);

        if (coding == null) {
            return null;
        }

        CodeDef codeDef = new CodeDef(coding.getSystem(), coding.getVersion(), coding.getCode(), coding.getDisplay());

        return new ConceptDef(List.of(codeDef), null);
    }

    private void checkIds(MeasureGroupComponent group) {
        group.getPopulation().forEach(R4MeasureDefBuilder::checkId);
    }

    /**
     * Validate ratio continuous variable measure structure if applicable.
     *
     * <p>For ratio continuous variable measures, validates:
     * - Exactly 2 MEASURE_OBSERVATION populations exist
     * - Each MEASURE_OBSERVATION has a criteria reference extension
     * - One MEASURE_OBSERVATION references NUMERATOR, one references DENOMINATOR
     *
     * @param measure the Measure resource
     * @param group the MeasureGroupComponent to validate
     * @param groupScoring the group-level scoring (may be null)
     * @param measureScoring the measure-level scoring
     */
    private void validateRatioContinuousVariableIfApplicable(
            Measure measure, MeasureGroupComponent group, MeasureScoring groupScoring, MeasureScoring measureScoring) {

        var effectiveScoring = R4MeasureUtils.computeScoring(measure.getUrl(), measureScoring, groupScoring);

        if (!R4MeasureUtils.isRatioContinuousVariable(effectiveScoring, group)) {
            return;
        }

        var measureObservations = R4MeasureUtils.getMeasureObservationPopulations(group);

        // Validate exactly 2 measure observations
        if (measureObservations.size() != 2) {
            throw new InvalidRequestException(
                    "Ratio Continuous Variable requires 2 Measure Observations defined, you have: %s"
                            .formatted(measureObservations.size()));
        }

        // Extract criteria references from both measure observations
        var criteriaRef1 = R4MeasureUtils.getCriteriaReferenceFromPopulation(measureObservations.get(0));
        var criteriaRef2 = R4MeasureUtils.getCriteriaReferenceFromPopulation(measureObservations.get(1));

        // Both must have criteria references
        if (criteriaRef1 == null) {
            throw new InvalidRequestException(
                    "MEASURE_OBSERVATION population with id '%s' is missing criteria reference extension for Measure: %s"
                            .formatted(measureObservations.get(0).getId(), measure.getUrl()));
        }
        if (criteriaRef2 == null) {
            throw new InvalidRequestException(
                    "MEASURE_OBSERVATION population with id '%s' is missing criteria reference extension for Measure: %s"
                            .formatted(measureObservations.get(1).getId(), measure.getUrl()));
        }

        // Verify criteria references exist as population IDs
        // If they don't, skip this validation - getCriteriaReference will catch that error later
        var criteriaRef1ExistsAsPopId =
                group.getPopulation().stream().map(Element::getId).anyMatch(id -> id.equals(criteriaRef1));
        var criteriaRef2ExistsAsPopId =
                group.getPopulation().stream().map(Element::getId).anyMatch(id -> id.equals(criteriaRef2));

        if (!criteriaRef1ExistsAsPopId || !criteriaRef2ExistsAsPopId) {
            // Let getCriteriaReference handle this validation
            return;
        }

        // One must reference numerator, one must reference denominator
        var hasNumeratorRef = R4MeasureUtils.criteriaReferenceMatches(criteriaRef1, MeasurePopulationType.NUMERATOR)
                || R4MeasureUtils.criteriaReferenceMatches(criteriaRef2, MeasurePopulationType.NUMERATOR);
        var hasDenominatorRef = R4MeasureUtils.criteriaReferenceMatches(criteriaRef1, MeasurePopulationType.DENOMINATOR)
                || R4MeasureUtils.criteriaReferenceMatches(criteriaRef2, MeasurePopulationType.DENOMINATOR);

        if (!hasNumeratorRef || !hasDenominatorRef) {
            throw new InvalidRequestException(
                    ("Ratio Continuous Variable requires one MEASURE_OBSERVATION to reference '%s' "
                                    + "and one to reference '%s', but found criteria references: '%s' and '%s' for Measure: %s")
                            .formatted(
                                    MeasurePopulationType.NUMERATOR.toCode(),
                                    MeasurePopulationType.DENOMINATOR.toCode(),
                                    criteriaRef1,
                                    criteriaRef2,
                                    measure.getUrl()));
        }
    }

    @Nonnull
    private PopulationDef buildPopulationDef(
            MeasureGroupPopulationComponent population,
            MeasureGroupComponent group,
            String measureUrl,
            CodeDef populationBasis,
            @Nullable List<SupportingEvidenceDef> supportingEvidenceDefs) {
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
                aggregateMethod,
                supportingEvidenceDefs);
    }

    // TODO: JM, DateOfCompliance can now be more simply exposed via supporting evidence instead of this current
    // workflow. Should deprecate.
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
                populationBasis,
                null);

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
    private StratifierDef buildStratifierDef(
            String measureUrl, MeasureGroupStratifierComponent mgsc, CodeDef populationBasisDef) {
        checkId(mgsc);

        boolean isBooleanBasis = isBooleanPopulationBasis(populationBasisDef);
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
                getStratifierType(measureUrl, mgsc, isBooleanBasis),
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

    @Nullable
    private static MeasureStratifierType getStratifierType(
            String measureUrl,
            MeasureGroupStratifierComponent measureGroupStratifierComponent,
            boolean isBooleanBasis) {
        if (measureGroupStratifierComponent == null) {
            return null;
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
        } else if (!isBooleanBasis) {
            return MeasureStratifierType.NON_SUBJECT_VALUE;
        } else {
            return MeasureStratifierType.VALUE;
        }
    }

    private static void triggerFirstPassValidation(Measure measure) {

        checkId(measure);

        // Validate unique population IDs within each group
        validateUniquePopulationIds(measure);

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

    /**
     * Validates that all population IDs within each group are unique.
     *
     * @param measure the Measure to validate
     * @throws InvalidRequestException if duplicate population IDs exist within a group
     */
    private static void validateUniquePopulationIds(Measure measure) {
        String measureIdentifier = measure.hasUrl() ? measure.getUrl() : measure.getId();

        for (int groupIndex = 0; groupIndex < measure.getGroup().size(); groupIndex++) {
            MeasureGroupComponent group = measure.getGroup().get(groupIndex);
            String groupIdentifier = group.hasId() ? group.getId() : "group-" + (groupIndex + 1);

            Set<String> seenPopulationIds = new HashSet<>();

            for (MeasureGroupPopulationComponent population : group.getPopulation()) {
                String populationId = population.getId();

                // Skip null/blank IDs - they will be caught by checkId() validation
                if (populationId == null || populationId.isBlank()) {
                    continue;
                }

                if (!seenPopulationIds.add(populationId)) {
                    throw new InvalidRequestException("Duplicate population ID '%s' found in %s of Measure: %s"
                            .formatted(populationId, groupIdentifier, measureIdentifier));
                }
            }
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
