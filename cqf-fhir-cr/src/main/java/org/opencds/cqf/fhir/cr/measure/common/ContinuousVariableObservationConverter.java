package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * Convert continuous variable scoring function-returned resources to Observations and Quantities in
 * a FHIR version specific way.
 */
@SuppressWarnings("squid:S1135")
public interface ContinuousVariableObservationConverter<T extends ICompositeType> {

    // TODO:  LD:  We need to come up with something other than an Observation to wrap FHIR Quantities
    //    QuantityHolder<T> wrapResultAsQuantityHolder(String id, Object result);
    T wrapResultAsQuantityHolder(String id, Object result);
}
