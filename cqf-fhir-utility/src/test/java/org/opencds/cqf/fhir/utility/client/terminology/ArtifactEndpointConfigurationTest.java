package org.opencds.cqf.fhir.utility.client.terminology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

class ArtifactEndpointConfigurationTest {

    private static final String VSAC_ROUTE = "https://cts.nlm.nih.gov/fhir";
    private static final String VSAC_VALUESET_ROUTE = "https://cts.nlm.nih.gov/fhir/ValueSet";
    private static final String EXAMPLE_ROUTE = "https://example.org/fhir";

    private static final String VSAC_VALUESET_URL =
            "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001";
    private static final String EXAMPLE_VALUESET_URL = "https://example.org/fhir/ValueSet/my-valueset";

    @Test
    void getMatchScore_exactRouteMatch_returnsRouteLength() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com", null);

        int score = config.getMatchScore(VSAC_VALUESET_URL);

        assertEquals(VSAC_ROUTE.length(), score);
    }

    @Test
    void getMatchScore_longerRouteMatch_returnsLongerScore() {
        var shortRouteConfig = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com", null);
        var longRouteConfig = new ArtifactEndpointConfiguration(VSAC_VALUESET_ROUTE, "https://tx.server.com", null);

        int shortScore = shortRouteConfig.getMatchScore(VSAC_VALUESET_URL);
        int longScore = longRouteConfig.getMatchScore(VSAC_VALUESET_URL);

        assertTrue(longScore > shortScore, "Longer matching route should have higher score");
        assertEquals(VSAC_ROUTE.length(), shortScore);
        assertEquals(VSAC_VALUESET_ROUTE.length(), longScore);
    }

    @Test
    void getMatchScore_noMatch_returnsNegativeOne() {
        var config = new ArtifactEndpointConfiguration(EXAMPLE_ROUTE, "https://tx.server.com", null);

        int score = config.getMatchScore(VSAC_VALUESET_URL);

        assertEquals(-1, score, "Non-matching route should return -1");
    }

    @Test
    void getMatchScore_noArtifactRoute_returnsZero() {
        var config = new ArtifactEndpointConfiguration(null, "https://tx.server.com", null);

        int score = config.getMatchScore(VSAC_VALUESET_URL);

        assertEquals(0, score, "No artifactRoute should return 0 (fallback)");
    }

    @Test
    void getMatchScore_emptyArtifactRoute_returnsZero() {
        var config = new ArtifactEndpointConfiguration("", "https://tx.server.com", null);

        int score = config.getMatchScore(VSAC_VALUESET_URL);

        assertEquals(0, score, "Empty artifactRoute should return 0 (fallback)");
    }

    @Test
    void getMatchScore_nullCanonicalUrl_returnsNegativeOne() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com", null);

        int score = config.getMatchScore(null);

        assertEquals(-1, score, "Null canonical URL should return -1");
    }

    @Test
    void getEffectiveEndpoint_withEndpointUri_createsEndpoint() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com/fhir", null);
        var fhirContext = FhirContext.forR4Cached();

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNotNull(endpoint);
        assertEquals("https://tx.server.com/fhir", endpoint.getAddress());
    }

    @Test
    void getEffectiveEndpoint_withEndpointResource_returnsEndpoint() {
        var fhirContext = FhirContext.forR4Cached();
        var factory = IAdapterFactory.forFhirContext(fhirContext);
        var r4Endpoint = new org.hl7.fhir.r4.model.Endpoint();
        r4Endpoint.setAddress("https://my.endpoint.com/fhir");
        var endpointAdapter = factory.createEndpoint(r4Endpoint);

        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, null, endpointAdapter);

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNotNull(endpoint);
        assertEquals("https://my.endpoint.com/fhir", endpoint.getAddress());
    }

    @Test
    void getEffectiveEndpoint_withBothEndpointAndUri_prefersEndpoint() {
        var fhirContext = FhirContext.forR4Cached();
        var factory = IAdapterFactory.forFhirContext(fhirContext);
        var r4Endpoint = new org.hl7.fhir.r4.model.Endpoint();
        r4Endpoint.setAddress("https://endpoint.resource.com/fhir");
        var endpointAdapter = factory.createEndpoint(r4Endpoint);

        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://endpoint.uri.com/fhir", endpointAdapter);

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNotNull(endpoint);
        assertEquals("https://endpoint.resource.com/fhir", endpoint.getAddress());
    }

    @Test
    void getEffectiveEndpoint_withNeither_returnsNull() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, null, null);
        var fhirContext = FhirContext.forR4Cached();

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNull(endpoint);
    }

    @Test
    void getEffectiveEndpoint_dstu3_createsCorrectEndpoint() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com/fhir", null);
        var fhirContext = FhirContext.forDstu3Cached();

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNotNull(endpoint);
        assertEquals("https://tx.server.com/fhir", endpoint.getAddress());
    }

    @Test
    void getEffectiveEndpoint_r5_createsCorrectEndpoint() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com/fhir", null);
        var fhirContext = FhirContext.forR5Cached();

        var endpoint = config.getEffectiveEndpoint(fhirContext);

        assertNotNull(endpoint);
        assertEquals("https://tx.server.com/fhir", endpoint.getAddress());
    }

    @Test
    void toString_includesAllFields() {
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://tx.server.com", null);

        var str = config.toString();

        assertTrue(str.contains(VSAC_ROUTE));
        assertTrue(str.contains("https://tx.server.com"));
    }
}
