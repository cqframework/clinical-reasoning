package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;

/**
 * Shared utilities for scoring strategies that compute continuous variable aggregates.
 */
final class ScoringUtils {

    private ScoringUtils() {}

    /**
     * Calculate continuous variable aggregate quantity.
     * Delegates to {@link MeasureScoreCalculator} for collection and aggregation.
     *
     * @param populationDef the population definition containing observation data
     * @param popDefToResources function to extract resources from population def
     * @return aggregated QuantityDef or null if population is null
     */
    @Nullable
    static QuantityDef calculateContinuousVariableAggregateQuantity(
            @Nullable PopulationDef populationDef, Function<PopulationDef, Collection<Object>> popDefToResources) {

        if (populationDef == null) {
            return null;
        }

        return calculateContinuousVariableAggregateQuantity(
                populationDef.getAggregateMethod(), popDefToResources.apply(populationDef));
    }

    /**
     * Calculate continuous variable aggregate quantity.
     * Delegates to {@link MeasureScoreCalculator} for collection and aggregation.
     *
     * @param aggregateMethod the aggregation method (SUM, AVG, MIN, MAX, MEDIAN, COUNT)
     * @param qualifyingResources the resources containing QuantityDef observations
     * @return aggregated QuantityDef or null if no resources
     */
    @Nullable
    static QuantityDef calculateContinuousVariableAggregateQuantity(
            ContinuousVariableObservationAggregateMethod aggregateMethod, Collection<Object> qualifyingResources) {
        var observationQuantity = MeasureScoreCalculator.collectQuantities(qualifyingResources);
        return MeasureScoreCalculator.aggregateContinuousVariable(observationQuantity, aggregateMethod);
    }
}
