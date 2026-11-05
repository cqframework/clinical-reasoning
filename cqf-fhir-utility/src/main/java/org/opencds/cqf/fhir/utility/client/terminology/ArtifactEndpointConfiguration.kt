package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import java.util.*
import org.hl7.fhir.dstu3.model.Endpoint
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.Companion.forFhirContext
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter

/**
 * Represents a CRMI artifact endpoint configuration used to route artifact resolution to
 * appropriate terminology/content servers based on canonical URL matching.
 *
 * @see
 *   [ CRMI Artifact Endpoint Configurable Operation](https://build.fhir.org/ig/HL7/crmi-ig/StructureDefinition-crmi-artifact-endpoint-configurable-operation.html)
 */
class ArtifactEndpointConfiguration(
    private val artifactRoute: String?,
    private val endpointUri: String?,
    private val endpoint: IEndpointAdapter?,
) {
    /**
     * @return The artifact route used to determine whether this endpoint should handle artifacts
     *   whose canonical URL starts with this route
     */
    fun getArtifactRoute(): Optional<String> {
        return Optional.ofNullable<String>(artifactRoute)
    }

    /** @return The URI of the endpoint (mutually exclusive with endpoint) */
    fun getEndpointUri(): Optional<String> {
        return Optional.ofNullable<String>(endpointUri)
    }

    /** @return The Endpoint resource (mutually exclusive with endpointUri) */
    fun getEndpoint(): Optional<IEndpointAdapter> {
        return Optional.ofNullable<IEndpointAdapter>(endpoint)
    }

    /**
     * Returns the effective endpoint adapter, creating one from endpointUri if needed.
     *
     * @param fhirContext the FHIR context for creating adapters
     * @return the endpoint adapter, or null if neither endpoint nor endpointUri is set
     */
    fun getEffectiveEndpoint(fhirContext: FhirContext): IEndpointAdapter? {
        if (endpoint != null) {
            return endpoint
        }
        if (endpointUri != null) {
            return createEndpointFromUri(fhirContext, endpointUri)
        }
        return null
    }

    /**
     * Calculates the match score for a canonical URL based on CRMI routing rules.
     *
     * @param canonicalUrl the canonical URL to match against
     * @return -1 if no match, 0 if no artifactRoute (fallback), or positive number representing the
     *   number of matching characters
     */
    fun getMatchScore(canonicalUrl: String?): Int {
        if (canonicalUrl == null) {
            return -1
        }

        // If no artifactRoute, this is a fallback endpoint (rank lower)
        if (artifactRoute.isNullOrEmpty()) {
            return 0
        }

        // Check if canonical URL starts with artifactRoute
        if (canonicalUrl.startsWith(artifactRoute)) {
            return artifactRoute.length
        }

        // No match
        return -1
    }

    override fun toString(): String {
        return ("ArtifactEndpointConfiguration{" +
            "artifactRoute='" +
            artifactRoute +
            '\'' +
            ", endpointUri='" +
            endpointUri +
            '\'' +
            ", endpoint=" +
            (if (endpoint != null) endpoint.address else "null") +
            '}')
    }

    companion object {
        /** Creates a minimal Endpoint adapter from a URI string. */
        private fun createEndpointFromUri(
            fhirContext: FhirContext,
            uri: String?,
        ): IEndpointAdapter {
            val factory = forFhirContext(fhirContext)
            when (fhirContext.version.version) {
                FhirVersionEnum.DSTU3 -> {
                    val dstu3Endpoint = Endpoint()
                    dstu3Endpoint.setAddress(uri)
                    return factory.createEndpoint(dstu3Endpoint)
                }

                FhirVersionEnum.R4 -> {
                    val r4Endpoint = org.hl7.fhir.r4.model.Endpoint()
                    r4Endpoint.setAddress(uri)
                    return factory.createEndpoint(r4Endpoint)
                }

                FhirVersionEnum.R5 -> {
                    val r5Endpoint = org.hl7.fhir.r5.model.Endpoint()
                    r5Endpoint.setAddress(uri)
                    return factory.createEndpoint(r5Endpoint)
                }

                else ->
                    throw IllegalArgumentException(
                        "Unsupported FHIR version: " + fhirContext.version.version
                    )
            }
        }
    }
}
