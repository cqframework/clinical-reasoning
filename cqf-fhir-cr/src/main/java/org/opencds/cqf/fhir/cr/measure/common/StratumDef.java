package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import java.util.Set;

/**
 * Equivalent to StratifierDef, but for Stratum.
 * <p/>
 * For now, this contains the code text and stratum population defs, in order to help with
 * continuous variable scoring, as well as other stratifier use cases, and is meant to be
 * the source of truth for all data points regarding stratum.
 */
public class StratumDef {
    // Equivalent to the FHIR stratum code text
    private final String text;
    private final List<StratumPopulationDef> stratumPopulations;
    private final Set<StratumValueDef> stratumValueDefs;
    private final List<String> subjectIds;

    public StratumDef(
            String text,
            List<StratumPopulationDef> stratumPopulations,
            Set<StratumValueDef> stratumValueDefs,
            List<String> subjectIds) {
        this.text = text;
        this.stratumPopulations = List.copyOf(stratumPopulations);
        this.stratumValueDefs = stratumValueDefs;
        this.subjectIds = subjectIds;
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

    public List<String> getSubjectIds() {
        return subjectIds;
    }
}
