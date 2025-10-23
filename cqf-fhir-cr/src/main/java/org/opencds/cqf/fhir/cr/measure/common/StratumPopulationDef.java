package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Set;

// LUKETODO:  javadoc
// LUKETODO:  more fields
// LUKETODO:  this merges the concept of a Stratum and a Stratum population so we'll have to decouple these
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
