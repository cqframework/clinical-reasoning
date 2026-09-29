package org.opencds.cqf.fhir.utility.adapter.r5

import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r5.model.Endpoint
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter

class EndpointAdapter(endpoint: IBaseResource) : ResourceAdapter(endpoint), IEndpointAdapter {
    init {
        require(endpoint is Endpoint) {
            "resource passed as endpoint argument is not an Endpoint resource"
        }
    }
}
