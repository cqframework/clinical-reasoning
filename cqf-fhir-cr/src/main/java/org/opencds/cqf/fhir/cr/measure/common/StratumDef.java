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
 * Converted from record to class by Claude Sonnet 4.5 to support mutable score storage
 */
public class StratumDef {

    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> valueDefs;
    private final Collection<String> subjectIds;

    // Added by Claude Sonnet 4.5 - score storage for version-agnostic measure scoring
    @Nullable
    private Double measureScore;

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
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

    // Added by Claude Sonnet 4.5 - score storage methods for version-agnostic measure scoring
    public void setMeasureScore(Double score) {
        this.measureScore = score;
    }

    @Nullable
    public Double getMeasureScore() {
        return this.measureScore;
    }

    public boolean hasMeasureScore() {
        return this.measureScore != null;
    }
}
