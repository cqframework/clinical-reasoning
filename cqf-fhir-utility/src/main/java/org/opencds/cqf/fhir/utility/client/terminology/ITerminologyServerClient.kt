package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.dstu3.model.Enumerations
import org.hl7.fhir.instance.model.api.IBaseEnumFactory
import org.opencds.cqf.fhir.utility.Canonicals.getResourceType
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings

interface ITerminologyServerClient : ITerminologyProvider {
    val terminologyServerClientSettings: TerminologyServerClientSettings?

    fun initializeClientWithAuth(endpoint: IEndpointAdapter?): IGenericClient

    /**
     * Checks if the endpoint base URL matches the Terminology Server URL
     *
     * @param address expected ValueSet url
     * @return default to false, and expect server specific clients, e.g. VSAC, to override
     */
    fun isCanonicalMatch(address: String?): Boolean {
        return false
    }

    companion object {
        // Strips resource and id from the endpoint address URL, these are not needed as the client
        // constructs the URL.
        // Converts http URLs to https
        @JvmStatic
        fun getAddressBase(address: String, ctx: FhirContext): String {
            var address = address
            if (address.startsWith("http://")) {
                address = address.replaceFirst("http://", "https://")
            }
            // remove trailing slashes
            if (address.endsWith("/")) {
                address = address.substring(0, address.length - 1)
            }
            // check if URL is in the format [base URL]/[resource type]/[id]
            var maybeFhirType = getResourceType(address)
            if (!maybeFhirType.isNullOrBlank()) {
                val factory: IBaseEnumFactory<*> = getEnumFactory(ctx)
                try {
                    factory.fromCode(maybeFhirType)
                } catch (e: IllegalArgumentException) {
                    // check if URL is in the format [base URL]/[resource type]
                    val lastSlashIndex = address.lastIndexOf("/")
                    if (lastSlashIndex > 0) {
                        maybeFhirType = address.substring(lastSlashIndex + 1)
                        try {
                            factory.fromCode(maybeFhirType)
                        } catch (e2: IllegalArgumentException) {
                            return address
                        }
                    } else {
                        return address
                    }
                }
                address = address.substring(0, address.indexOf(maybeFhirType) - 1)
            }
            return address
        }

        fun getEnumFactory(ctx: FhirContext): IBaseEnumFactory<*> {
            return when (ctx.version.version) {
                FhirVersionEnum.DSTU3 -> Enumerations.ResourceTypeEnumFactory()
                FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Enumerations.ResourceTypeEnumFactory()
                FhirVersionEnum.R5 ->
                    org.hl7.fhir.r5.model.Enumerations.ResourceTypeEnumEnumFactory()
                else ->
                    throw UnprocessableEntityException(
                        "unsupported FHIR version: " + ctx.version.version.toString()
                    )
            }
        }

        const val versionParamName: String = "valueSetVersion"
        const val urlParamName: String = "url"
    }
}
