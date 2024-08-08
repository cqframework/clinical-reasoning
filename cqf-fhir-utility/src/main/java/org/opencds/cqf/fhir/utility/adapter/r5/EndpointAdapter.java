package org.opencds.cqf.fhir.utility.adapter.r5;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Endpoint;

public class EndpointAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.EndpointAdapter {
    public EndpointAdapter(IBaseResource endpoint) {
        super(endpoint);
        if (!(endpoint instanceof Endpoint)) {
            throw new IllegalArgumentException("resource passed as endpoint argument is not an Endpoint resource");
        }
    }

    public EndpointAdapter(Endpoint endpoint) {
        super(endpoint);
    }
}
