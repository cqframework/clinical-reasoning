package org.opencds.cqf.fhir.cr.measure.common;

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

    // Mutable score field for version-agnostic scoring
    private Double score;

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
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
     * Get the count for a specific PopulationDef in this stratum.
     *
     * @param populationDef the PopulationDef to match by ID
     * @return the count, or 0 if not found
     */
    public int getPopulationCount(PopulationDef populationDef) {
        StratumPopulationDef stratumPop = getStratumPopulation(populationDef);
        return stratumPop != null ? stratumPop.getCount() : 0;
    }

    /**
     * Get the computed score for this stratum.
     * Used by version-agnostic MeasureDefScorer.
     *
     * @return the score, or null if not yet computed
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * Set the computed score for this stratum.
     * Used by version-agnostic MeasureDefScorer to store computed scores.
     *
     * @param score the computed score
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * Get the measure score for this stratum, applying improvement notation.
     *
     * Note: Improvement notation is applied at GroupDef level, not per-stratum.
     * Stratifiers inherit improvement notation from their parent group.
     *
     * @return the score with improvement notation applied, or null if no score
     */
    public Double getMeasureScore() {
        if (this.score == null) {
            return null;
        }

        // Note: Improvement notation applied at GroupDef level, not per-stratum
        // Stratifiers inherit improvement notation from their parent group
        return this.score;
    }

    /**
     * Creates a deep copy snapshot of this StratumDef including mutable score field.
     *
     * Copies all collections (stratumPopulations, valueDefs, subjectIds) and the mutable score field.
     *
     * @return A new StratumDef instance with deep copied collections and copied score
     */
    public StratumDef createSnapshot() {
        List<StratumPopulationDef> snapshotPopulations = List.copyOf(stratumPopulations);
        Set<StratumValueDef> snapshotValueDefs = Set.copyOf(valueDefs);
        Collection<String> snapshotSubjectIds = List.copyOf(subjectIds);

        // Create snapshot using constructor
        StratumDef snapshot = new StratumDef(snapshotPopulations, snapshotValueDefs, snapshotSubjectIds);

        // Copy mutable score field using setter
        snapshot.setScore(this.score);

        return snapshot;
    }
}
