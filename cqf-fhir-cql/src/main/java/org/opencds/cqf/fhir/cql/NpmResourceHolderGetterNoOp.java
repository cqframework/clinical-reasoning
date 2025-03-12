package org.opencds.cqf.fhir.cql;

import org.hl7.fhir.r4.model.CanonicalType;

public enum NpmResourceHolderGetterNoOp implements NpmResourceHolderGetter {
    INSTANCE;

    @Override
    public NpmResourceHolder loadNpmResources(CanonicalType measureUrl) {
        return NpmResourceHolder.EMPTY;
    }
}
