package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * Scoring strategy for PROPORTION and standard RATIO measures (without MEASUREOBSERVATION).
 * Computes score as: (numerator - numeratorExclusion) / (denominator - denominatorExclusion - denominatorException).
 */
class ProportionRatioScoringStrategy implements ScoringStrategy {

    @Override
    @Nullable
    public Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureEvaluationState state) {
        return MeasureScoreCalculator.calculateProportionScore(
                getPopulationCount(groupDef, MeasurePopulationType.NUMERATOR, state),
                getPopulationCount(groupDef, MeasurePopulationType.NUMERATOREXCLUSION, state),
                getPopulationCount(groupDef, MeasurePopulationType.DENOMINATOR, state),
                getPopulationCount(groupDef, MeasurePopulationType.DENOMINATOREXCLUSION, state),
                getPopulationCount(groupDef, MeasurePopulationType.DENOMINATOREXCEPTION, state));
    }

    @Override
    @Nullable
    public Double calculateStratumScore(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureEvaluationState state) {
        return scoreProportionRatioStratum(groupDef, stratumDef);
    }

    /**
     * Score a stratum for standard PROPORTION or RATIO measures (non-continuous variable).
     * Uses simple numerator/denominator count ratio.
     */
    @Nullable
    private static Double scoreProportionRatioStratum(GroupDef groupDef, StratumDef stratumDef) {
        int numeratorCount = stratumDef.getPopulationCount(groupDef.getSingle(MeasurePopulationType.NUMERATOR));
        int denominatorCount = stratumDef.getPopulationCount(groupDef.getSingle(MeasurePopulationType.DENOMINATOR));

        // Pass 0 for exclusions since they're already applied at stratum level
        return MeasureScoreCalculator.calculateProportionScore(numeratorCount, 0, denominatorCount, 0, 0);
    }

    /**
     * Look up a population count from state by iterating the group's populations.
     */
    private static int getPopulationCount(GroupDef groupDef, MeasurePopulationType type, MeasureEvaluationState state) {
        for (PopulationDef pop : groupDef.populations()) {
            if (pop.type() == type) {
                return state.population(pop).getCount();
            }
        }
        return 0;
    }
}
