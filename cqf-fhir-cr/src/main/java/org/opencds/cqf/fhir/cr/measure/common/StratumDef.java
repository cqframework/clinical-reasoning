package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

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

    public StratumDef(String text, List<StratumPopulationDef> stratumPopulations) {
        this.text = text;
        this.stratumPopulations = List.copyOf(stratumPopulations);
    }

    public String getText() {
        return text;
    }

    public List<StratumPopulationDef> getStratumPopulations() {
        return stratumPopulations;
    }
}
