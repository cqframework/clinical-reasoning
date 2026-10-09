package org.opencds.cqf.fhir.utility.adapter.dstu3

import org.hl7.fhir.dstu3.model.Endpoint
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter

class EndpointAdapter(endpoint: IBaseResource) :
    ResourceAdapter(endpoint as Resource), IEndpointAdapter {
    init {
        require(endpoint is Endpoint) {
            "resource passed as endpoint argument is not an Endpoint resource"
        }
    }
}
