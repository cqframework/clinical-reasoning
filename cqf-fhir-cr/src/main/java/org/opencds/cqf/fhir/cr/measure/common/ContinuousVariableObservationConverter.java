package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Convert continuous variable scoring function-returned resources to Observations and Quantities in
 * a FHIR version specific way.
 */
public interface ContinuousVariableObservationConverter<T extends IBaseResource> {

    // TODO:  LD:  We need to come up with something other than an Observation to wrap FHIR Quantities
    T wrapResultAsObservation(String id, String observationName, Object result);
}
