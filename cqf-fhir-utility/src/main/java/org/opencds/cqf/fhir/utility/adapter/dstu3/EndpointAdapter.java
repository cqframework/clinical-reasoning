package org.opencds.cqf.fhir.utility.adapter.dstu3;

import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class EndpointAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.EndpointAdapter {
    public EndpointAdapter(IBaseResource endpoint) {
        super(endpoint);
        if (!(endpoint instanceof Endpoint)) {
            throw new IllegalArgumentException("resource passed as endpoint argument is not an Endpoint resource");
        }
    }
}