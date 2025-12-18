package org.opencds.cqf.fhir.utility.adapter;

import ca.uhn.fhir.context.FhirContext;

public interface IBaseAdapterTest {

    /**
     * Returns the fhir context for use
     */
    FhirContext fhirContext();

    /**
     * Return the AdapterFactory to use
     */
    IAdapterFactory getAdapterFactory();

    /**
     * Returns the canonical reference for an embedded RelatedArtifact type
     */
    default String toRelatedArtifactCanonicalReference(String ref) {
        return "\"" + ref + "\"";
    }
}
