package org.opencds.cqf.fhir.cql;

import org.hl7.fhir.r4.model.CanonicalType;

// LUKETODO:  think if I really need an interface to live in clinical-reasoning
public interface NpmResourceHolderGetter {

    // LUKETODO:  unit test this:
    NpmResourceHolder loadNpmResources(CanonicalType measureUrl);
}
