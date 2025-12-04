package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.common.BaseMeasureReportScorer;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
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
 * Evaluation of Measure Report Data showing raw CQL criteria results compared to resulting Measure Report.
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

    // Added by Claude Sonnet 4.5 on 2025-11-28 to facilitate future refactoring
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
                    // Refactored by Claude Sonnet 4.5 on 2025-12-02 to use GroupDef.getPopulationCount()
                    score = calcProportionScore(
                            groupDef.getPopulationCount(MeasurePopulationType.NUMERATOR)
                                    - groupDef.getPopulationCount(MeasurePopulationType.NUMERATOREXCLUSION),
                            groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOR)
                                    - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCLUSION)
                                    - groupDef.getPopulationCount(MeasurePopulationType.DENOMINATOREXCEPTION));
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

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to work with QuantityDef
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

        if (den == null || den == 0.0) {
            return null;
        }

        if (num == null || num == 0.0) {
            // Explicitly handle numerator zero with positive denominator
            return den > 0.0 ? 0.0 : null;
        }

        return num / den;
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to convert QuantityDef at the end
    protected void scoreContinuousVariable(
            String measureUrl, MeasureReportGroupComponent mrgc, GroupDef groupDef, PopulationDef populationDef) {
        final QuantityDef aggregateQuantityDef = calculateContinuousVariableAggregateQuantity(
                measureUrl, populationDef, PopulationDef::getAllSubjectResources);

        // Convert QuantityDef to R4 Quantity at the last moment before setting on report
        Quantity aggregateQuantity = continuousVariableConverter.convertToFhirQuantity(aggregateQuantityDef);
        mrgc.setMeasureScore(aggregateQuantity);
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to return QuantityDef
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

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to return QuantityDef
    @Nullable
    private static QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Object> qualifyingResources) {
        var observationQuantity = collectQuantities(qualifyingResources);
        return aggregate(observationQuantity, aggregateMethod);
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to work with QuantityDef
    private static QuantityDef aggregate(
            List<QuantityDef> quantities, ContinuousVariableObservationAggregateMethod method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        double result;

        switch (method) {
            case SUM:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .sum();
                break;
            case MAX:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .max()
                        .orElse(Double.NaN);
                break;
            case MIN:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .min()
                        .orElse(Double.NaN);
                break;
            case AVG:
                result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .average()
                        .orElse(Double.NaN);
                break;
            case COUNT:
                result = quantities.size();
                break;
            case MEDIAN:
                List<Double> sorted = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
                int n = sorted.size();
                if (n % 2 == 1) {
                    result = sorted.get(n / 2);
                } else {
                    result = (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported aggregation method: " + method);
        }

        return new QuantityDef(result);
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to collect QuantityDef
    private static List<QuantityDef> collectQuantities(Collection<Object> resources) {

        var mapValues = resources.stream()
                .filter(x -> x instanceof Map<?, ?>)
                .map(x -> (Map<?, ?>) x)
                .map(Map::values)
                .flatMap(Collection::stream)
                .toList();

        return mapValues.stream()
                .filter(QuantityDef.class::isInstance)
                .map(QuantityDef.class::cast)
                .toList();
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
                // Updated by Claude Sonnet 4.5 on 2025-12-02
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
                    // Refactored by Claude Sonnet 4.5 on 2025-12-02 to use StratumPopulationDef.getCount()
                    score = calcProportionScore(
                            getCountFromStratifierPopulation(groupDef, stratumDef, MeasurePopulationType.NUMERATOR),
                            getCountFromStratifierPopulation(groupDef, stratumDef, MeasurePopulationType.DENOMINATOR));
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
                // Enhanced by Claude Sonnet 4.5 on 2025-11-27 - convert QuantityDef to Quantity
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

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27 to work with QuantityDef
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

        if (den == null || den == 0.0) {
            return null;
        }

        if (num == null || num == 0.0) {
            // Explicitly handle numerator zero with positive denominator
            return den > 0.0 ? 0.0 : null;
        }

        return num / den;
    }

    /**
     * Updated by Claude Sonnet 4.5 on 2025-12-02
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
}
