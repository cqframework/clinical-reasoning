package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;

public class EndpointAdapter extends ResourceAdapter implements IEndpointAdapter {
    public EndpointAdapter(IBaseResource endpoint) {
        super(endpoint);
        if (!(endpoint instanceof Endpoint)) {
            throw new InvalidRequestException("resource passed as endpoint argument is not an Endpoint resource");
        }
    }
}
