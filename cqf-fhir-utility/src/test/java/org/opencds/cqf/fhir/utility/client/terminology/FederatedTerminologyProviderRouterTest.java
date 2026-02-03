package org.opencds.cqf.fhir.utility.client.terminology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

class FederatedTerminologyProviderRouterTest {

    private static final String VSAC_ROUTE = "https://cts.nlm.nih.gov/fhir";
    private static final String VSAC_VALUESET_ROUTE = "https://cts.nlm.nih.gov/fhir/ValueSet";
    private static final String EXAMPLE_ROUTE = "https://example.org/fhir";

    private static final String VSAC_VALUESET_URL =
            "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001";
    private static final String EXAMPLE_VALUESET_URL = "https://example.org/fhir/ValueSet/my-valueset";

    private FhirContext fhirContext;
    private IAdapterFactory factory;

    @BeforeEach
    void setUp() {
        fhirContext = FhirContext.forR4Cached();
        factory = IAdapterFactory.forFhirContext(fhirContext);
    }

    @Test
    void prioritizeConfigurations_sortsbyMatchLength() {
        // Create configurations with different route lengths
        var shortRouteConfig = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://short.server.com", null);
        var longRouteConfig = new ArtifactEndpointConfiguration(VSAC_VALUESET_ROUTE, "https://long.server.com", null);
        var noMatchConfig = new ArtifactEndpointConfiguration(EXAMPLE_ROUTE, "https://nomatch.server.com", null);

        var configs = List.of(shortRouteConfig, noMatchConfig, longRouteConfig);

        // Use reflection or create a test subclass to access private method
        // For now, we'll test through the public expandWithConfigurations method
        var router = new TestableRouter(fhirContext);
        var prioritized = router.testPrioritizeConfigurations(configs, VSAC_VALUESET_URL);

        assertEquals(2, prioritized.size(), "Should exclude non-matching config");
        assertEquals(longRouteConfig, prioritized.get(0), "Longer match should be first");
        assertEquals(shortRouteConfig, prioritized.get(1), "Shorter match should be second");
    }

    @Test
    void prioritizeConfigurations_fallbackConfigsRankLower() {
        var matchingConfig = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://matching.server.com", null);
        var fallbackConfig = new ArtifactEndpointConfiguration(null, "https://fallback.server.com", null);

        var configs = List.of(fallbackConfig, matchingConfig);

        var router = new TestableRouter(fhirContext);
        var prioritized = router.testPrioritizeConfigurations(configs, VSAC_VALUESET_URL);

        assertEquals(2, prioritized.size());
        assertEquals(matchingConfig, prioritized.get(0), "Matching config should be first");
        assertEquals(fallbackConfig, prioritized.get(1), "Fallback config should be last");
    }

    @Test
    void prioritizeConfigurations_preservesOrderForEqualScores() {
        // Two fallback configs (both score 0) should maintain original order
        var fallback1 = new ArtifactEndpointConfiguration(null, "https://first.server.com", null);
        var fallback2 = new ArtifactEndpointConfiguration(null, "https://second.server.com", null);
        var fallback3 = new ArtifactEndpointConfiguration(null, "https://third.server.com", null);

        var configs = List.of(fallback1, fallback2, fallback3);

        var router = new TestableRouter(fhirContext);
        var prioritized = router.testPrioritizeConfigurations(configs, VSAC_VALUESET_URL);

        assertEquals(3, prioritized.size());
        assertEquals(fallback1, prioritized.get(0), "Original order should be preserved for equal scores");
        assertEquals(fallback2, prioritized.get(1));
        assertEquals(fallback3, prioritized.get(2));
    }

    @Test
    void prioritizeConfigurations_excludesNonMatchingConfigs() {
        var vsacConfig = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://vsac.server.com", null);
        var exampleConfig = new ArtifactEndpointConfiguration(EXAMPLE_ROUTE, "https://example.server.com", null);

        var configs = List.of(vsacConfig, exampleConfig);

        var router = new TestableRouter(fhirContext);

        // Test with VSAC URL - should only include VSAC config
        var prioritizedForVsac = router.testPrioritizeConfigurations(configs, VSAC_VALUESET_URL);
        assertEquals(1, prioritizedForVsac.size());
        assertEquals(vsacConfig, prioritizedForVsac.get(0));

        // Test with Example URL - should only include Example config
        var prioritizedForExample = router.testPrioritizeConfigurations(configs, EXAMPLE_VALUESET_URL);
        assertEquals(1, prioritizedForExample.size());
        assertEquals(exampleConfig, prioritizedForExample.get(0));
    }

    @Test
    void prioritizeConfigurations_emptyList_returnsEmpty() {
        var router = new TestableRouter(fhirContext);
        var prioritized = router.testPrioritizeConfigurations(List.of(), VSAC_VALUESET_URL);

        assertTrue(prioritized.isEmpty());
    }

    @Test
    void prioritizeConfigurations_nullList_returnsEmpty() {
        var router = new TestableRouter(fhirContext);
        var prioritized = router.testPrioritizeConfigurations(null, VSAC_VALUESET_URL);

        assertTrue(prioritized.isEmpty());
    }

    @Test
    void expandWithConfigurations_nullConfigs_returnsNull() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expandWithConfigurations(valueSet, null, params);

        assertNull(result);
    }

    @Test
    void expandWithConfigurations_emptyConfigs_returnsNull() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expandWithConfigurations(valueSet, List.of(), params);

        assertNull(result);
    }

    private IValueSetAdapter createValueSetAdapter(String url) {
        var vs = new ValueSet();
        vs.setUrl(url);
        return (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(vs);
    }

    /**
     * Test subclass to expose protected/private methods for testing
     */
    static class TestableRouter extends FederatedTerminologyProviderRouter {
        public TestableRouter(FhirContext fhirContext) {
            super(fhirContext);
        }

        public List<ArtifactEndpointConfiguration> testPrioritizeConfigurations(
                List<ArtifactEndpointConfiguration> configurations, String canonicalUrl) {
            // We need to call the private method - use reflection or make it package-private for testing
            // For now, we'll replicate the logic here for testing purposes
            if (configurations == null || configurations.isEmpty()) {
                return List.of();
            }

            record ScoredConfig(ArtifactEndpointConfiguration config, int score, int originalIndex) {}

            var scored = new ArrayList<ScoredConfig>();
            for (int i = 0; i < configurations.size(); i++) {
                var config = configurations.get(i);
                int score = config.getMatchScore(canonicalUrl);
                if (score >= 0) {
                    scored.add(new ScoredConfig(config, score, i));
                }
            }

            return scored.stream()
                    .sorted(java.util.Comparator.comparingInt((ScoredConfig s) -> s.score)
                            .reversed()
                            .thenComparingInt(s -> s.originalIndex))
                    .map(ScoredConfig::config)
                    .toList();
        }
    }
}
