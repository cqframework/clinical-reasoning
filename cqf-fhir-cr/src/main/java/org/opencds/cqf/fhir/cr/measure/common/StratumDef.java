package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.List;

// LUKETODO:  javadoc
public class StratumDef {
    private final String text;
    private final List<StratumPopulationDef> stratumPopulations;

    public StratumDef(String text, List<StratumPopulationDef> stratumPopulations) {
        this.text = text;
        this.stratumPopulations = stratumPopulations;
    }

    public String getText() {
        return text;
    }

    public List<StratumPopulationDef> getStratumPopulations() {
        return stratumPopulations;
    }

    public void addStratumPopulation(StratumPopulationDef stratumPopulationDef) {
        stratumPopulations.add(stratumPopulationDef);
    }

    public void addAllPopulations(ArrayList<StratumPopulationDef> stratumPopulationDefs) {
        stratumPopulations.addAll(stratumPopulationDefs);
    }
}
