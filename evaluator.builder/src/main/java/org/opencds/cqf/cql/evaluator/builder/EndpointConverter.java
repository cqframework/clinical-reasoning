package org.opencds.cqf.cql.evaluator.builder;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface EndpointConverter {
    public EndpointInfo getEndpointInfo(IBaseResource endpoint);
}

