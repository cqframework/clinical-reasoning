package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * Convert continuous variable scoring function-returned resources to Observations and Quantities in
 * a FHIR version specific way.
 */
@SuppressWarnings("squid:S1135")
public interface ContinuousVariableObservationConverter<T extends ICompositeType> {

    T wrapResultAsQuantity(Object result);
}
