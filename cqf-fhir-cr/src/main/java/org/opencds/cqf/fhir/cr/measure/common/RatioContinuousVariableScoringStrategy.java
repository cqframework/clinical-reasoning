package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Scoring strategy for RATIO measures with MEASUREOBSERVATION populations.
 * Handles continuous variable ratio scoring where numerator and denominator have separate observations.
 */
class RatioContinuousVariableScoringStrategy implements ScoringStrategy {

    @Override
    @Nullable
    public Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureEvaluationState state) {
        return scoreRatioMeasureObservationGroup(groupDef, state);
    }

    @Override
    @Nullable
    public Double calculateStratumScore(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureEvaluationState state) {
        return scoreRatioMeasureObservationStratum(measureUrl, stratumDef, state);
    }

    /**
     * Score a group for RATIO measures with MEASUREOBSERVATION populations.
     * Handles continuous variable ratio scoring where numerator and denominator have separate observations.
     */
    @Nullable
    private static Double scoreRatioMeasureObservationGroup(GroupDef groupDef, MeasureEvaluationState state) {
        // Get all MEASUREOBSERVATION populations
        var measureObservationPopulationDefs = groupDef.getPopulationDefs(MeasurePopulationType.MEASUREOBSERVATION);

        // Find Measure Observations for Numerator and Denominator
        final PopulationDef numeratorPopulation =
                findPopulationDef(groupDef, measureObservationPopulationDefs, MeasurePopulationType.NUMERATOR);
        final PopulationDef denominatorPopulation =
                findPopulationDef(groupDef, measureObservationPopulationDefs, MeasurePopulationType.DENOMINATOR);

        // Calculate aggregate quantities for numerator and denominator
        final QuantityDef numeratorAggregate = ScoringUtils.calculateContinuousVariableAggregateQuantity(
                numeratorPopulation, pop -> state.population(pop).getAllSubjectResources());
        final QuantityDef denominatorAggregate = ScoringUtils.calculateContinuousVariableAggregateQuantity(
                denominatorPopulation, pop -> state.population(pop).getAllSubjectResources());

        // If there's no numerator or not denominator result, we still want to capture the
        // other result
        setAggregateResultIfPopNonNull(numeratorPopulation, numeratorAggregate, state);
        setAggregateResultIfPopNonNull(denominatorPopulation, denominatorAggregate, state);

        if (numeratorAggregate == null || denominatorAggregate == null) {
            return null;
        }

        return MeasureScoreCalculator.calculateRatioScore(numeratorAggregate.value(), denominatorAggregate.value());
    }

    /**
     * Score a stratum for RATIO measures with MEASUREOBSERVATION populations.
     * Uses pre-computed cache to eliminate redundant lookups during scoring.
     */
    @Nullable
    private static Double scoreRatioMeasureObservationStratum(
            String measureUrl, StratumDef stratumDef, MeasureEvaluationState state) {

        if (stratumDef == null) {
            return null;
        }

        // Use pre-computed cache - eliminates all lookups
        MeasureObservationStratumCache cache = stratumDef.getMeasureObservationCache();
        if (cache == null) {
            return null;
        }

        // Extract cached references
        StratumPopulationDef stratumPopulationDefNum = cache.numeratorObservation();
        StratumPopulationDef stratumPopulationDefDen = cache.denominatorObservation();

        // Get parent PopulationDefs directly from StratumPopulationDef
        PopulationDef numPopDef = stratumPopulationDefNum.populationDef();
        PopulationDef denPopDef = stratumPopulationDefDen.populationDef();

        return scoreRatioContVariableStratum(
                measureUrl, stratumPopulationDefNum, stratumPopulationDefDen, numPopDef, denPopDef, state);
    }

    /**
     * Score ratio continuous variable for a stratum.
     */
    private static Double scoreRatioContVariableStratum(
            String measureUrl,
            StratumPopulationDef measureObsNumStratum,
            StratumPopulationDef measureObsDenStratum,
            PopulationDef numPopDef,
            PopulationDef denPopDef,
            MeasureEvaluationState state) {

        // Calculate aggregate for numerator observations filtered by stratum
        QuantityDef aggregateNumQuantityDef = ScoringUtils.calculateContinuousVariableAggregateQuantity(
                numPopDef,
                populationDef -> StratumResultsHelper.getResultsForStratum(populationDef, measureObsNumStratum, state));

        // Calculate aggregate for denominator observations filtered by stratum
        QuantityDef aggregateDenQuantityDef = ScoringUtils.calculateContinuousVariableAggregateQuantity(
                denPopDef,
                populationDef -> StratumResultsHelper.getResultsForStratum(populationDef, measureObsDenStratum, state));

        // Persist per-stratum aggregation results BEFORE the null check so that
        // valid results are saved even when the other population has no observations.
        // StratumPopulationDef stays mutable (created fresh each evaluation)
        if (aggregateNumQuantityDef != null && aggregateNumQuantityDef.value() != null) {
            measureObsNumStratum.setAggregationResult(aggregateNumQuantityDef.value());
        }
        if (aggregateDenQuantityDef != null && aggregateDenQuantityDef.value() != null) {
            measureObsDenStratum.setAggregationResult(aggregateDenQuantityDef.value());
        }

        if (aggregateNumQuantityDef == null || aggregateDenQuantityDef == null) {
            return null;
        }

        Double num = aggregateNumQuantityDef.value();
        Double den = aggregateDenQuantityDef.value();

        if (num == null || den == null) {
            return null;
        }

        // Delegate ratio scoring to MeasureScoreCalculator
        return MeasureScoreCalculator.calculateRatioScore(num, den);
    }

    /**
     * Find PopulationDef by matching criteria reference.
     */
    @Nullable
    private static PopulationDef findPopulationDef(
            GroupDef groupDef, List<PopulationDef> populationDefs, MeasurePopulationType type) {
        PopulationDef firstPop = groupDef.getFirstWithTypeAndNonNullId(type);
        if (firstPop == null) {
            return null;
        }

        String criteriaId = firstPop.id();

        return populationDefs.stream()
                .filter(populationDef -> criteriaId.equals(populationDef.getCriteriaReference()))
                .findFirst()
                .orElse(null);
    }

    private static void setAggregateResultIfPopNonNull(
            @Nullable PopulationDef populationDef, QuantityDef quantityDef, MeasureEvaluationState state) {
        Optional.ofNullable(populationDef).ifPresent(nonNullPopulationDef -> {
            state.population(nonNullPopulationDef).setAggregationResult(quantityDef);
        });
    }
}
