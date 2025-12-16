package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoreCalculator;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumPopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueDef;
import org.opencds.cqf.fhir.cr.measure.common.StratumValueWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * R4-specific FHIR MeasureReport scoring utilities.
 *
 * <p><strong>INTERNAL USAGE DEPRECATED (2025-12-16):</strong> Internal measure evaluation now uses
 * {@link org.opencds.cqf.fhir.cr.measure.common.MeasureReportDefScorer} integrated into the workflow.
 * This class is retained ONLY for external callers who may depend on its public API.
 *
 * <p><strong>EXTERNAL CALLERS:</strong> If you are using this class directly, be aware that it is
 * maintained for backward compatibility only. A proper external API for measure scoring will be
 * implemented in a future release. Please contact the maintainers if you have a use case that
 * requires this functionality.
 *
 * <p><strong>KEY METHODS FOR EXTERNAL USE:</strong>
 * <ul>
 *   <li>{@link #getCountFromStratifierPopulation(List, String)} - Get population count from stratifier</li>
 *   <li>{@link #getCountFromGroupPopulation(List, String)} - Get population count from group</li>
 * </ul>
 *
 * <p>See: integrate-measure-def-scorer-part2-integration PRP for migration details.
 *
 * <p>Each row represents a subject as raw cql criteria expression output:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    | F  |    |    | Not in Initial Population or Denominator
 * G       | G  | G  |    | G  | G  |    | InDenominatorException = true & InNumerator = true
 * }</pre>
 *
 * <p>Each row represents a subject and their inclusion/exclusion population criteria on a Measure Report:
 *
 * <pre>{@code
 * Subject | IP | D  | DX | N  | DE | NX | Notes
 * --------|----|----|----|----|----|----|---------------------------------------------------------------
 * A       | A  | A  | A  |    |    |    |
 * B       | B  | B  |    | B  |    |    |
 * C       | C  | C  |    |    | C  |    | InDenominator = true, InDenominatorException = true,
 *                                       | InNumerator = false → Scores as InDenominatorException = true
 * D       | D  | D  |    | D  |    | D  |
 * E       | E  | E  |    | E  |    |    |
 * F       |    |    |    |    |    |    | Excluded: Not in Initial Population or Denominator
 * G       | G  | G  |    | G  |    |    | InDenominatorException = true & InNumerator = true → Remove from DE
 * }</pre>
 *
 * <p><strong>Population Counts:</strong>
 * <ul>
 *   <li>Initial Population (ip): 6</li>
 *   <li>Denominator (d): 6</li>
 *   <li>Denominator Exclusion (dx): 1</li>
 *   <li>Numerator (n): 4</li>
 *   <li>Denominator Exception (de): 1</li>
 *   <li>Numerator Exclusion (nx): 1</li>
 * </ul>
 *
 * <p><strong>Performance Rate Formula:</strong><br>
 * {@code (n - nx) / (d - dx - de)}<br>
 * {@code (4 - 1) / (6 - 1 - 1)} = <b>0.75</b>
 *
 * <p><strong>Measure Score:</strong> {@code 0.75}<br>
 *
 * <p> (v3.18.0 and below) Previous calculation of measure score from MeasureReport only interpreted Numerator, Denominator membership since exclusions and exceptions were already applied. Now exclusions and exceptions are present in Denominator and Numerator populations, the measure scorer calculation has to take into account additional population membership to determine Final-Numerator and Final-Denominator values</p>
 */
@SuppressWarnings("squid:S1135")
public class R4MeasureReportScorer extends BaseMeasureReportScorer<MeasureReport> {

    private static final Logger logger = LoggerFactory.getLogger(R4MeasureReportScorer.class);

    private final ContinuousVariableObservationConverter<Quantity> continuousVariableConverter =
            R4ContinuousVariableObservationConverter.INSTANCE;

    @Override
    public void score(String measureUrl, MeasureDef measureDef, MeasureReport measureReport) {
        // Measure Def Check
        if (measureDef == null) {
            throw new InvalidRequestException(
                    "MeasureDef is required in order to score a Measure for Measure: " + measureUrl);
        }
        // No groups to score, nothing to do.
        if (measureReport.getGroup().isEmpty()) {
            return;
        }

        for (MeasureReportGroupComponent mrgc : measureReport.getGroup()) {
            scoreGroup(
                    measureUrl,
                    getGroupMeasureScoring(mrgc, measureDef),
                    mrgc,
                    getGroupDef(measureDef, mrgc).isIncreaseImprovementNotation(),
                    getGroupDef(measureDef, mrgc));
        }
    }

    protected GroupDef getGroupDef(MeasureDef measureDef, MeasureReportGroupComponent mrgc) {
        var groupDefs = measureDef.groups();
        if (groupDefs.size() == 1) {
            return groupDefs.get(0);
        }
        return groupDefs.stream()
                .filter(t -> t.id().equals(mrgc.getId()))
                .findFirst()
                .orElse(null);
    }

    protected MeasureScoring getGroupMeasureScoring(MeasureReportGroupComponent mrgc, MeasureDef measureDef) {
        MeasureScoring groupScoringType = null;
        // if not multi-rate, get first groupDef scoringType
        if (measureDef.groups().size() == 1) {
            groupScoringType = measureDef.groups().get(0).measureScoring();
        } else {
            // multi rate measure, match groupComponent to groupDef to extract scoringType
            for (GroupDef groupDef : measureDef.groups()) {
                var groupDefMeasureScoring = groupDef.measureScoring();
                // groups must have id populated
                groupHasValidId(measureDef, mrgc.getId());
                groupHasValidId(measureDef, groupDef.id());
                // Match by group id if available
                // Note: Match by group's population id, was removed per cqf-measures conformance change FHIR-45423
                // multi-rate measures must have a group.id defined to be conformant
                if (groupDef.id().equals(mrgc.getId())) {
                    groupScoringType = groupDefMeasureScoring;
                }
            }
        }
        return checkMissingScoringType(measureDef, groupScoringType);
    }

    protected void scoreGroup(Double score, boolean isIncreaseImprovementNotation, MeasureReportGroupComponent mrgc) {
        // When applySetMembership=false, this value can receive strange values
        // This should prevent scoring in certain scenarios like <0
        if (score != null && score >= 0) {
            if (isIncreaseImprovementNotation) {
                mrgc.setMeasureScore(new Quantity(score));
            } else {
                mrgc.setMeasureScore(new Quantity(1 - score));
            }
        }
    }

    protected void scoreGroup(
            String measureUrl,
            MeasureScoring measureScoring,
            MeasureReportGroupComponent mrgc,
            boolean isIncreaseImprovementNotation,
            GroupDef groupDef) {

        switch (measureScoring) {
            case PROPORTION, RATIO:
                Double score;
                // Ratio Continuous Variable Scoring
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    score = scoreRatioContVariable(measureUrl, groupDef, getMeasureObservations(groupDef));
                } else {
                    // Standard Proportion & Ratio Scoring
                    // Delegate to MeasureScoreCalculator
                    score = MeasureScoreCalculator.calculateProportionScore(
                            getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR),
                            getCountFromGroupPopulation(mrgc.getPopulation(), NUMERATOR_EXCLUSION),
                            getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR),
                            getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCLUSION),
                            getCountFromGroupPopulation(mrgc.getPopulation(), DENOMINATOR_EXCEPTION));
                }
                scoreGroup(score, isIncreaseImprovementNotation, mrgc);
                break;

            case CONTINUOUSVARIABLE:
                // increase notation cannot be applied to ContVariable
                scoreContinuousVariable(measureUrl, mrgc, groupDef, getFirstMeasureObservation(groupDef));
                break;
            default:
                break;
        }

        for (MeasureReportGroupStratifierComponent stratifierComponent : mrgc.getStratifier()) {
            scoreStratifier(measureUrl, groupDef, measureScoring, stratifierComponent);
        }
    }

    @Nullable
    protected Double scoreRatioContVariable(String measureUrl, GroupDef groupDef, List<PopulationDef> populationDefs) {

        // Defensive checks
        if (groupDef == null || populationDefs == null || populationDefs.isEmpty()) {
            return null;
        }

        PopulationDef numPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.NUMERATOR);
        PopulationDef denPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.DENOMINATOR);

        if (numPopDef == null || denPopDef == null) {
            return null;
        }

        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, numPopDef, PopulationDef::getAllSubjectResources);
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, denPopDef, PopulationDef::getAllSubjectResources);

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (num == null || den == null) {
            return null;
        }

        // Delegate to MeasureScoreCalculator
        return MeasureScoreCalculator.calculateRatioScore(num, den);
    }

    protected void scoreContinuousVariable(
            String measureUrl, MeasureReportGroupComponent mrgc, GroupDef groupDef, PopulationDef populationDef) {
        final QuantityDef aggregateQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, populationDef, PopulationDef::getAllSubjectResources);

        // Convert QuantityDef to R4 Quantity at the last moment before setting on report
        Quantity aggregateQuantity = continuousVariableConverter.convertToFhirQuantity(aggregateQuantityDef);
        mrgc.setMeasureScore(aggregateQuantity);
    }

    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            String measureUrl,
            PopulationDef populationDef,
            Function<PopulationDef, Collection<Object>> popDefToResources) {

        if (populationDef == null) {
            // In the case where we're missing a measure population definition, we don't want to
            // throw an Exception, but we want the existing error handling to include this
            // error in the MeasureReport output.
            logger.warn("Measure population group has no measure population defined for measure: {}", measureUrl);
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                populationDef.getAggregateMethod(), popDefToResources.apply(populationDef));
    }

    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Object> qualifyingResources) {
        // Delegate to MeasureScoreCalculator for collection and aggregation
        var observationQuantity = MeasureScoreCalculator.collectQuantities(qualifyingResources);
        return MeasureScoreCalculator.aggregateContinuousVariable(observationQuantity, aggregateMethod);
    }

    protected void scoreStratifier(
            String measureUrl,
            GroupDef groupDef,
            MeasureScoring measureScoring,
            MeasureReportGroupStratifierComponent stratifierComponent) {

        for (StratifierGroupComponent sgc : stratifierComponent.getStratum()) {

            // This isn't fantastic, but it seems to work
            final Optional<StratifierDef> optStratifierDef = groupDef.stratifiers().stream()
                    .filter(stratifierDef -> stratifierComponent.getId().equals(stratifierDef.id()))
                    .findFirst();

            if (optStratifierDef.isEmpty()) {
                throw new InternalErrorException("Stratifier component " + sgc.getId() + " does not exist.");
            }

            final StratifierDef stratifierDef = optStratifierDef.get();

            final StratumDef stratumDef = stratifierDef.getStratum().stream()
                    .filter(stratumDefInner -> doesStratumDefMatchStratum(sgc, stratifierDef, stratumDefInner))
                    .findFirst()
                    .orElse(null);

            // TODO: LD: should we always expect these to match up?
            if (stratumDef == null) {
                logger.warn("stratumDef is null");
            }

            scoreStratum(measureUrl, groupDef, stratumDef, measureScoring, sgc);
        }
    }

    // TODO:  LD: consider refining this logic:
    private boolean doesStratumDefMatchStratum(
            StratifierGroupComponent sgc, StratifierDef stratifierDef, StratumDef stratumDefInner) {
        return Objects.equals(
                getStratumDefTextForR4(stratifierDef, stratumDefInner),
                sgc.getValue().getText());
    }

    private static String getStratumDefTextForR4(StratifierDef stratifierDef, StratumDef stratumDef) {
        String stratumText = null;

        for (StratumValueDef valuePair : stratumDef.valueDefs()) {
            var value = valuePair.value();
            var componentDef = valuePair.def();
            // Set Stratum value to indicate which value is displaying results
            // ex. for Gender stratifier, code 'Male'
            if (value.getValueClass().equals(CodeableConcept.class)) {
                if (stratumDef.isComponent()) {
                    // component stratifier example: code: "gender", value: 'M'
                    // value being stratified: 'M'
                    stratumText = componentDef.code().text();
                } else {
                    // non-component stratifiers only set stratified value, code is set on stratifier object
                    // value being stratified: 'M'
                    if (value.getValue() instanceof CodeableConcept codeableConcept) {
                        stratumText = codeableConcept.getText();
                    }
                }
            } else if (stratumDef.isComponent()) {
                stratumText = expressionResultToCodableConcept(value).getText();
            } else if (MeasureStratifierType.VALUE == stratifierDef.getStratifierType()) {
                // non-component stratifiers only set stratified value, code is set on stratifier object
                // value being stratified: 'M'
                stratumText = expressionResultToCodableConcept(value).getText();
            } else if (MeasureStratifierType.CRITERIA == stratifierDef.getStratifierType()) {
                // Handle CRITERIA-type stratifiers with non-CodeableConcept values (e.g., String, Boolean)
                stratumText = expressionResultToCodableConcept(value).getText();
            }
        }

        return stratumText;
    }

    // This is weird pattern where we have multiple qualifying values within a single stratum,
    // which was previously unsupported.  So for now, comma-delim the first five values.
    private static CodeableConcept expressionResultToCodableConcept(StratumValueWrapper value) {
        return new CodeableConcept().setText(value.getValueAsString());
    }

    protected void scoreStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef,
            MeasureScoring measureScoring,
            StratifierGroupComponent stratum) {
        final Quantity quantity = getStratumScoreOrNull(measureUrl, groupDef, stratumDef, measureScoring, stratum);

        if (quantity != null) {
            stratum.setMeasureScore(quantity);
        }
    }

    @Nullable
    private Quantity getStratumScoreOrNull(
            String measureUrl,
            GroupDef groupDef,
            StratumDef stratumDef,
            MeasureScoring measureScoring,
            StratifierGroupComponent stratum) {

        switch (measureScoring) {
            case PROPORTION, RATIO -> {
                Double score;
                // Ratio Continuous Variable Scoring
                if (measureScoring.equals(MeasureScoring.RATIO)
                        && groupDef.hasPopulationType(MeasurePopulationType.MEASUREOBSERVATION)) {
                    // new
                    final StratumPopulationDef stratumPopulationDefNum;
                    final StratumPopulationDef stratumPopulationDefDen;
                    PopulationDef numPopDef = null;
                    PopulationDef denPopDef = null;
                    if (stratumDef != null) {
                        var populationDefs = getMeasureObservations(groupDef);
                        // get Measure Observation for Numerator and Denominator
                        numPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.NUMERATOR);
                        denPopDef = findPopulationDef(groupDef, populationDefs, MeasurePopulationType.DENOMINATOR);
                        // assign Num & Den stratum Populations
                        stratumPopulationDefDen = getStratumPopDefFromPopDef(stratumDef, denPopDef);
                        stratumPopulationDefNum = getStratumPopDefFromPopDef(stratumDef, numPopDef);

                    } else {
                        stratumPopulationDefNum = null;
                        stratumPopulationDefDen = null;
                    }

                    score = scoreRatioContVariableStratum(
                            measureUrl,
                            groupDef,
                            stratumPopulationDefNum,
                            stratumPopulationDefDen,
                            numPopDef,
                            denPopDef);
                } else {
                    // Standard Proportion & Ratio Scoring
                    // Delegate to MeasureScoreCalculator (pass 0 for exclusions since already applied at stratum level)
                    score = MeasureScoreCalculator.calculateProportionScore(
                            getCountFromStratifierPopulation(stratum.getPopulation(), NUMERATOR),
                            0,
                            getCountFromStratifierPopulation(stratum.getPopulation(), DENOMINATOR),
                            0,
                            0);
                }
                if (score != null) {
                    return new Quantity(score);
                }
                return null;
            }
            case CONTINUOUSVARIABLE -> {
                final StratumPopulationDef stratumPopulationDef;
                if (stratumDef != null) {
                    stratumPopulationDef = stratumDef.stratumPopulations().stream()
                            // Ex:  match "measure-observation-1" with "measure-observation"
                            .filter(stratumPopDef ->
                                    stratumPopDef.id().startsWith(MeasurePopulationType.MEASUREOBSERVATION.toCode()))
                            .findFirst()
                            .orElse(null);
                } else {
                    stratumPopulationDef = null;
                }
                QuantityDef quantityDef = calculateContinuousVariableAggregateQuantity(
                        measureUrl,
                        getFirstMeasureObservation(groupDef),
                        populationDef -> getResultsForStratum(populationDef, stratumPopulationDef));
                return continuousVariableConverter.convertToFhirQuantity(quantityDef);
            }
            default -> {
                return null;
            }
        }
    }

    @Nullable
    protected Double scoreRatioContVariableStratum(
            String measureUrl,
            GroupDef groupDef,
            StratumPopulationDef measureObsNumStratum,
            StratumPopulationDef measureObsDenStratum,
            PopulationDef numPopDef,
            PopulationDef denPopDef) {

        QuantityDef aggregateNumQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, numPopDef, populationDef -> getResultsForStratum(populationDef, measureObsNumStratum));
        calculateContinuousVariableAggregateQuantity(measureUrl, numPopDef, PopulationDef::getAllSubjectResources);
        QuantityDef aggregateDenQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, denPopDef, populationDef -> getResultsForStratum(populationDef, measureObsDenStratum));

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (num == null || den == null) {
            return null;
        }

        // Delegate to MeasureScoreCalculator
        return MeasureScoreCalculator.calculateRatioScore(num, den);
    }

    /**
     * Get count from StratumDef's StratumPopulationDef.
     * Optimized to use GroupDef.getSingle() and StratumDef.getPopulationCount().
     *
     * @param groupDef the GroupDef containing the group information
     * @param stratumDef the StratumDef containing stratum populations (may be null)
     * @param populationType the MeasurePopulationType to find
     * @return the count for the stratum population, or 0 if not found
     */
    private int getCountFromStratifierPopulation(
            GroupDef groupDef, StratumDef stratumDef, MeasurePopulationType populationType) {
        if (stratumDef == null) {
            return 0;
        }

        // TODO:  LD:  we need to make this matching more sophisticated, since we could have two
        // MeasureObservations in the result, one for numerator, and one for denominator
        PopulationDef populationDef = groupDef.getSingle(populationType);
        return stratumDef.getPopulationCount(populationDef);
    }

    /**
     * Restored from commit 9874c95 by Claude Sonnet 4.5 on 2025-12-16 (Phase 2)
     * Get count from R4 FHIR MeasureReportGroupPopulationComponent list.
     * This is the R4-specific implementation that works directly with MeasureReport objects.
     * Used for group-level proportion/ratio scoring.
     *
     * @param populations the list of R4 FHIR MeasureReportGroupPopulationComponents
     * @param populationName the population code to find (e.g., "numerator", "denominator")
     * @return the count for the population, or 0 if not found
     */
    private int getCountFromGroupPopulation(
            List<MeasureReportGroupPopulationComponent> populations, String populationName) {
        return populations.stream()
                .filter(population -> populationName.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(MeasureReportGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }

    /**
     * Restored from commit 9874c95 by Claude Sonnet 4.5 on 2025-12-16 (Phase 1)
     * Get count from R4 FHIR StratifierGroupPopulationComponent list.
     * This is the R4-specific implementation that works directly with MeasureReport objects.
     * Used for stratifier-level proportion/ratio scoring.
     *
     * @param populations the list of R4 FHIR StratifierGroupPopulationComponents
     * @param populationName the population code to find (e.g., "numerator", "denominator")
     * @return the count for the population, or 0 if not found
     */
    private int getCountFromStratifierPopulation(
            List<StratifierGroupPopulationComponent> populations, String populationName) {
        return populations.stream()
                .filter(population -> populationName.equals(
                        population.getCode().getCodingFirstRep().getCode()))
                .map(StratifierGroupPopulationComponent::getCount)
                .findAny()
                .orElse(0);
    }
}
