package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Set;

// TODO: LD:  more fields
/**
 * Equivalent to the FHIR stratum population.
 * <p/>
 * For now, this contains only an id to help match it to the population def in question and subjects
 * used to
 */
public class StratumPopulationDef {

    private final String id;
    private final Set<String> subjects;

    public StratumPopulationDef(String id, Set<String> subjects) {
        this.id = id;
        this.subjects = subjects;
    }

    public String getId() {
        return id;
    }

    public Set<String> getSubjects() {
        return subjects;
    }
}
