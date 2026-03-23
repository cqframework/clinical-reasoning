package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * Scoring strategy for CONTINUOUSVARIABLE measures.
 * Aggregates MEASUREOBSERVATION population observations using the population's aggregate method.
 */
class ContinuousVariableScoringStrategy implements ScoringStrategy {

    @Override
    @Nullable
    public Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureEvaluationState state) {
        final PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);

        if (measureObsPop == null) {
            return null;
        }

        final QuantityDef quantityDef = scoreContinuousVariable(measureObsPop, state);

        // Record the aggregate result for later computation for continuous variable reports
        state.population(measureObsPop).setAggregationResult(quantityDef);

        return quantityDef != null ? quantityDef.value() : null;
    }

    @Override
    @Nullable
    public Double calculateStratumScore(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureEvaluationState state) {
        return scoreContinuousVariableStratum(measureUrl, groupDef, stratumDef, state);
    }

    /**
     * Score continuous variable measure - returns QuantityDef with aggregated value.
     */
    private static QuantityDef scoreContinuousVariable(PopulationDef populationDef, MeasureEvaluationState state) {
        return ScoringUtils.calculateContinuousVariableAggregateQuantity(
                populationDef, pop -> state.population(pop).getAllSubjectResources());
    }

    /**
     * Score a stratum for CONTINUOUSVARIABLE measures.
     * Aggregates MEASUREOBSERVATION population observations filtered by stratum subjects.
     */
    @Nullable
    private static Double scoreContinuousVariableStratum(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureEvaluationState state) {

        // Get the MEASUREOBSERVATION population from GroupDef
        PopulationDef measureObsPop = groupDef.getSingle(MeasurePopulationType.MEASUREOBSERVATION);
        if (measureObsPop == null) {
            return null;
        }

        // Find the stratum population corresponding to MEASUREOBSERVATION
        StratumPopulationDef stratumPopulationDef = stratumDef.stratumPopulations().stream()
                .filter(stratumPopDef -> MeasurePopulationType.MEASUREOBSERVATION
                        == stratumPopDef.populationDef().type())
                .findFirst()
                .orElse(null);

        if (stratumPopulationDef == null) {
            return null;
        }

        // Calculate aggregate using stratum-filtered resources
        QuantityDef quantityDef = ScoringUtils.calculateContinuousVariableAggregateQuantity(
                measureObsPop,
                populationDef -> StratumResultsHelper.getResultsForStratum(populationDef, stratumPopulationDef, state));

        // Persist per-stratum aggregation result for downstream aggregation
        // StratumPopulationDef stays mutable (created fresh each evaluation)
        if (quantityDef != null && quantityDef.value() != null) {
            stratumPopulationDef.setAggregationResult(quantityDef.value());
        }

        return quantityDef != null ? quantityDef.value() : null;
    }
}
