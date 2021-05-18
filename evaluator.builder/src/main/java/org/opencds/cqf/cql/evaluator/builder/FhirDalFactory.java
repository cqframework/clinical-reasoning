package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

public interface FhirDalFactory {
    /**
     * Returns a FhirDal for the given endpointInfo. Returns null if
     * endpointInfo is null.
     * 
     * Override in subclasses to provide "default" behavior for your platform.
     * 
     * @param endpointInfo the EndpointInfo for the location of FHIR resources
     * @return a FhirDal
     */
    FhirDal create(EndpointInfo endpointInfo);
}
