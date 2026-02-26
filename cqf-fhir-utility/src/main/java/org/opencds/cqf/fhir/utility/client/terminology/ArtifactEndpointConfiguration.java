package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirContext;
import java.util.Optional;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;

/**
 * Represents a CRMI artifact endpoint configuration used to route artifact resolution
 * to appropriate terminology/content servers based on canonical URL matching.
 *
 * @see <a href="https://build.fhir.org/ig/HL7/crmi-ig/StructureDefinition-crmi-artifact-endpoint-configurable-operation.html">
 *      CRMI Artifact Endpoint Configurable Operation</a>
 */
public class ArtifactEndpointConfiguration {

    private final String artifactRoute;
    private final String endpointUri;
    private final IEndpointAdapter endpoint;

    public ArtifactEndpointConfiguration(String artifactRoute, String endpointUri, IEndpointAdapter endpoint) {
        this.artifactRoute = artifactRoute;
        this.endpointUri = endpointUri;
        this.endpoint = endpoint;
    }

    /**
     * @return The artifact route used to determine whether this endpoint should handle
     *         artifacts whose canonical URL starts with this route
     */
    public Optional<String> getArtifactRoute() {
        return Optional.ofNullable(artifactRoute);
    }

    /**
     * @return The URI of the endpoint (mutually exclusive with endpoint)
     */
    public Optional<String> getEndpointUri() {
        return Optional.ofNullable(endpointUri);
    }

    /**
     * @return The Endpoint resource (mutually exclusive with endpointUri)
     */
    public Optional<IEndpointAdapter> getEndpoint() {
        return Optional.ofNullable(endpoint);
    }

    /**
     * Returns the effective endpoint adapter, creating one from endpointUri if needed.
     *
     * @param fhirContext the FHIR context for creating adapters
     * @return the endpoint adapter, or null if neither endpoint nor endpointUri is set
     */
    public IEndpointAdapter getEffectiveEndpoint(FhirContext fhirContext) {
        if (endpoint != null) {
            return endpoint;
        }
        if (endpointUri != null) {
            return createEndpointFromUri(fhirContext, endpointUri);
        }
        return null;
    }

    /**
     * Calculates the match score for a canonical URL based on CRMI routing rules.
     *
     * @param canonicalUrl the canonical URL to match against
     * @return -1 if no match, 0 if no artifactRoute (fallback), or positive number
     *         representing the number of matching characters
     */
    public int getMatchScore(String canonicalUrl) {
        if (canonicalUrl == null) {
            return -1;
        }

        // If no artifactRoute, this is a fallback endpoint (rank lower)
        if (artifactRoute == null || artifactRoute.isEmpty()) {
            return 0;
        }

        // Check if canonical URL starts with artifactRoute
        if (canonicalUrl.startsWith(artifactRoute)) {
            return artifactRoute.length();
        }

        // No match
        return -1;
    }

    /**
     * Creates a minimal Endpoint adapter from a URI string.
     */
    private static IEndpointAdapter createEndpointFromUri(FhirContext fhirContext, String uri) {
        var factory = org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.forFhirContext(fhirContext);
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3:
                var dstu3Endpoint = new org.hl7.fhir.dstu3.model.Endpoint();
                dstu3Endpoint.setAddress(uri);
                return factory.createEndpoint(dstu3Endpoint);
            case R4:
                var r4Endpoint = new org.hl7.fhir.r4.model.Endpoint();
                r4Endpoint.setAddress(uri);
                return factory.createEndpoint(r4Endpoint);
            case R5:
                var r5Endpoint = new org.hl7.fhir.r5.model.Endpoint();
                r5Endpoint.setAddress(uri);
                return factory.createEndpoint(r5Endpoint);
            default:
                throw new IllegalArgumentException(
                        "Unsupported FHIR version: " + fhirContext.getVersion().getVersion());
        }
    }

    @Override
    public String toString() {
        return "ArtifactEndpointConfiguration{"
                + "artifactRoute='" + artifactRoute + '\''
                + ", endpointUri='" + endpointUri + '\''
                + ", endpoint=" + (endpoint != null ? endpoint.getAddress() : "null")
                + '}';
    }
}
