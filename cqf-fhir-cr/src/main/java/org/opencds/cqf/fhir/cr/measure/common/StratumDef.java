package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Equivalent to StratifierDef, but for Stratum.
 * <p/>
 * For now, this contains the code text and stratum population defs, in order to help with
 * continuous variable scoring, as well as other stratifier use cases, and is meant to be the source
 * of truth for all data points regarding stratum.
 * <p/>
 * Converted from record to class by Claude Sonnet 4.5 on 2025-12-03 to support mutable score field.
 */
public class StratumDef {

    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> valueDefs;
    private final Collection<String> subjectIds;
    private final MeasureObservationStratumCache measureObservationCache;

    // Added by Claude Sonnet 4.5 on 2025-12-03
    // Mutable score field for version-agnostic scoring
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

    // Record-style accessor methods (maintain compatibility)
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
     * Added by Claude Sonnet 4.5 on 2025-12-02
     * Get the StratumPopulationDef for a specific PopulationDef.
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
     * Added by Claude Sonnet 4.5 on 2025-12-02
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
     * Added by Claude Sonnet 4.5 on 2025-12-03
     * Get the computed score for this stratum.
     * Used by version-agnostic MeasureDefScorer.
     *
     * @return the score, or null if not yet computed
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-03
     * Set the computed score for this stratum.
     * Used by version-agnostic MeasureDefScorer to store computed scores.
     *
     * @param score the computed score
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-05
     * Get the pre-computed measure observation cache for this stratum.
     * Used by version-agnostic MeasureDefScorer to avoid redundant lookups during scoring.
     *
     * @return the cache, or null if not applicable (measures without observations linked to numerator/denominator)
     */
    @Nullable
    public MeasureObservationStratumCache getMeasureObservationCache() {
        return measureObservationCache;
    }
}
