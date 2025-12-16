package org.opencds.cqf.fhir.cr.measure.common.def.report;

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
public class StratumReportDef {

    private final List<StratumPopulationReportDef> stratumPopulations;
    private final Set<StratumValueReportDef> valueDefs;
    private final Collection<String> subjectIds;

    // Added by Claude Sonnet 4.5 on 2025-12-03
    // Mutable score field for version-agnostic scoring
    private Double score;

    public StratumReportDef(
            List<StratumPopulationReportDef> stratumPopulations,
            Set<StratumValueReportDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
    }

    // Record-style accessor methods (maintain compatibility)
    public List<StratumPopulationReportDef> stratumPopulations() {
        return stratumPopulations;
    }

    public Set<StratumValueReportDef> valueDefs() {
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
    public StratumPopulationReportDef getStratumPopulation(PopulationReportDef populationDef) {
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
    public int getPopulationCount(PopulationReportDef populationDef) {
        StratumPopulationReportDef stratumPop = getStratumPopulation(populationDef);
        return stratumPop != null ? stratumPop.getCount() : 0;
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
}
