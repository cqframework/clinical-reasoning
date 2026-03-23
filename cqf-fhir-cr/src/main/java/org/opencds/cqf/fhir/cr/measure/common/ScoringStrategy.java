package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * Strategy for calculating measure scores based on scoring type.
 * Implementations handle the specifics of PROPORTION, RATIO, and CONTINUOUSVARIABLE scoring.
 */
public interface ScoringStrategy {

    /**
     * Calculate the group-level score.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param state the mutable evaluation state
     * @return the calculated score or null
     */
    @Nullable
    Double calculateGroupScore(String measureUrl, GroupDef groupDef, MeasureEvaluationState state);

    /**
     * Calculate the stratum-level score.
     *
     * @param measureUrl the measure URL for error reporting
     * @param groupDef the group definition
     * @param stratumDef the stratum definition
     * @param state the mutable evaluation state
     * @return the calculated score or null
     */
    @Nullable
    Double calculateStratumScore(
            String measureUrl, GroupDef groupDef, StratumDef stratumDef, MeasureEvaluationState state);
}
