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
 *
 * @param text Equivalent to the FHIR stratum code text
 */
public record StratumDef(
        String text,
        List<StratumPopulationDef> stratumPopulations,
        Set<StratumValueDef> stratumValueDefs,
        Collection<String> subjectIds) {

    public StratumDef(
            String text,
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> stratumValueDefs,
            Collection<String> subjectIds) {
        this.text = text;
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.stratumValueDefs = stratumValueDefs;
        this.subjectIds = subjectIds;
    }

    public Set<StratumValueDef> getValueDefs() {
        return stratumValueDefs;
    }
}
