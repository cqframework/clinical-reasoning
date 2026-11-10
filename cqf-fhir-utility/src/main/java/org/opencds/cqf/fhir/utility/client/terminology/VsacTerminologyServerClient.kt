package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseExtension
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.client.Clients
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings

class VsacTerminologyServerClient : GenericTerminologyServerClient {
    constructor(fhirContext: FhirContext) : super(fhirContext)

    constructor(
        fhirContext: FhirContext,
        terminologyServerClientSettings: TerminologyServerClientSettings?,
    ) : super(fhirContext, terminologyServerClientSettings)

    override fun initializeClientWithAuth(endpoint: IEndpointAdapter?): IGenericClient {
        val fhirClient = super.initializeClientWithAuth(endpoint)

        // This smacks of reinventing the wheel... Should be passed in with the Endpoint resource
        // (Endpoint.header
        // element)... e.g. Basic {username:password (base64 encoded)}
        if (
            endpoint!!.hasExtension(Constants.VSAC_USERNAME) &&
                endpoint.hasExtension(Constants.APIKEY)
        ) {
            val username =
                endpoint
                    .getExtensionsByUrl<IBaseExtension<*, *>>(Constants.VSAC_USERNAME)
                    .firstOrNull()
                    ?.value
                    ?.toString()
                    ?: throw UnprocessableEntityException(
                        "Found a ${Constants.VSAC_USERNAME} extension with no value"
                    )
            val apiKey =
                endpoint
                    .getExtensionsByUrl<IBaseExtension<*, *>>(Constants.APIKEY)
                    .firstOrNull()
                    ?.value
                    ?.toString()
                    ?: throw UnprocessableEntityException(
                        "Found a ${Constants.APIKEY} extension with no value"
                    )
            Clients.registerAdditionalRequestHeadersAuth(fhirClient, username, apiKey)
        }

        return fhirClient
    }

    override fun isCanonicalMatch(address: String?): Boolean {
        return address!!.matches("https*://cts.nlm.nih.gov/fhir.*".toRegex())
    }
}
