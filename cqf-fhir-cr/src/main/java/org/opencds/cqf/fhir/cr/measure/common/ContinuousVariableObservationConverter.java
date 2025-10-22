package org.opencds.cqf.fhir.cr.measure.common;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface ContinuousVariableObservationConverter<T extends IBaseResource> {

    T wrapResultAsObservation(String id, String observationName, Object result);
}
