package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Per-stratum evaluation result, created during measure evaluation.
 * <p>
 * Unlike {@link GroupDef} and {@link PopulationDef} (which are immutable definitions from the
 * Measure resource), StratumDef instances are ephemeral: created per-evaluation by
 * {@link MeasureMultiSubjectEvaluator}, stored in {@link MeasureEvaluationState.StratifierState},
 * scored by {@link MeasureReportDefScorer}, and read by report builders.
 * <p>
 * The mutable {@code score} field is safe because each instance is single-owner within one
 * evaluation lifecycle.
 */
public class StratumDef {

    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> valueDefs;
    private final Collection<String> subjectIds;
    private final MeasureObservationStratumCache measureObservationCache;

    // Mutable: set by MeasureReportDefScorer during scoring
    private Double score;

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds,
            MeasureObservationStratumCache measureObservationCache) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
        this.measureObservationCache = measureObservationCache;
    }

    public List<StratumPopulationDef> stratumPopulations() {
        return stratumPopulations;
    }

    public Set<StratumValueDef> valueDefs() {
        return valueDefs;
    }

    public Collection<String> subjectIds() {
        return subjectIds;
    }

    public boolean isComponent() {
        return valueDefs.size() > 1;
    }

    /**
     * Find the StratumPopulationDef that corresponds to the given PopulationDef, matched by ID.
     *
     * @param populationDef the PopulationDef to match by ID
     * @return the StratumPopulationDef, or null if not found
     */
    public StratumPopulationDef getStratumPopulation(PopulationDef populationDef) {
        if (populationDef == null) {
            return null;
        }
        return stratumPopulations.stream()
                .filter(stratumPop -> stratumPop.id().equals(populationDef.id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the count for a specific PopulationDef in this stratum.
     *
     * @param populationDef the PopulationDef to match by ID
     * @return the count, or 0 if not found
     */
    public int getPopulationCount(PopulationDef populationDef) {
        StratumPopulationDef stratumPop = getStratumPopulation(populationDef);
        return stratumPop != null ? stratumPop.getCount() : 0;
    }

    public StratumPopulationDef findPopulationById(String populationId) {
        return this.stratumPopulations.stream()
                .filter(stratumPopulation -> populationId.equals(stratumPopulation.id()))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the computed score, or null if not yet scored
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * @param score the computed score
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * @return the cache, or null if not applicable (measures without observations)
     */
    @Nullable
    public MeasureObservationStratumCache getMeasureObservationCache() {
        return measureObservationCache;
    }
}
