package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Set;

/**
 * Equivalent to StratifierDef, but for Stratum.
 * <p/>
 * For now, this contains the code text and stratum population defs, in order to help with
 * continuous variable scoring, but will probably need to be enhanced for more use cases.
 */
public class StratumDef {
    // Equivalent to the FHIR stratum code text
    private final String text;
    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> stratumValueDefs;

    public StratumDef(
            String text, List<StratumPopulationDef> stratumPopulations, Set<StratumValueDef> stratumValueDefs) {
        this.text = text;
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.stratumValueDefs = stratumValueDefs;
    }

    public String getText() {
        return text;
    }

    public List<StratumPopulationDef> getStratumPopulations() {
        return stratumPopulations;
    }

    public Set<StratumValueDef> getValueDefs() {
        return stratumValueDefs;
    }
}
