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
 */
public record StratumDef(
        List<StratumPopulationDef> stratumPopulations, Set<StratumValueDef> valueDefs, Collection<String> subjectIds) {

    public StratumDef(
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> valueDefs,
            Collection<String> subjectIds) {
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.valueDefs = valueDefs;
        this.subjectIds = subjectIds;
    }
}
