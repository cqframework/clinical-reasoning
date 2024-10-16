package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;

public class EndpointAdapter extends ResourceAdapter implements IEndpointAdapter {
    public EndpointAdapter(IBaseResource endpoint) {
        super(endpoint);
        if (!(endpoint instanceof Endpoint)) {
            throw new IllegalArgumentException("resource passed as endpoint argument is not an Endpoint resource");
        }
    }
}
