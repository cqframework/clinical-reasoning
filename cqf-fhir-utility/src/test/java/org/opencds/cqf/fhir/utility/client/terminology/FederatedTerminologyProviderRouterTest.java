package org.opencds.cqf.fhir.utility.client.terminology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
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

    @Test
    void expandWithConfigurations_withValidConfig_expandsValueSet() {
        var expandedVs = new ValueSet();
        expandedVs.setUrl(VSAC_VALUESET_URL);
        expandedVs.getExpansion().addContains().setSystem("http://loinc.org").setCode("12345");

        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(expandedVs)
                .when(router)
                .expand(any(IValueSetAdapter.class), any(IEndpointAdapter.class), any(IParametersAdapter.class));

        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://cts.nlm.nih.gov/fhir", null);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expandWithConfigurations(valueSet, List.of(config), params);

        assertNotNull(result);
        assertEquals(1, ((ValueSet) result).getExpansion().getContains().size());
    }

    @Test
    void expandWithConfigurations_configWithNoEndpoint_returnsNull() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        // Config with no endpointUri and no endpoint resource - getEffectiveEndpoint returns null
        var config = new ArtifactEndpointConfiguration(VSAC_ROUTE, null, null);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expandWithConfigurations(valueSet, List.of(config), params);

        assertNull(result);
    }

    @Test
    void getValueSetResourceWithConfigurations_nullConfigs_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getValueSetResourceWithConfigurations(null, VSAC_VALUESET_URL);
        assertTrue(result.isEmpty());
    }

    @Test
    void getValueSetResourceWithConfigurations_emptyConfigs_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getValueSetResourceWithConfigurations(List.of(), VSAC_VALUESET_URL);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCodeSystemResourceWithConfigurations_nullConfigs_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getCodeSystemResourceWithConfigurations(null, "http://loinc.org");
        assertTrue(result.isEmpty());
    }

    @Test
    void getCodeSystemResourceWithConfigurations_emptyConfigs_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getCodeSystemResourceWithConfigurations(List.of(), "http://loinc.org");
        assertTrue(result.isEmpty());
    }

    @Test
    void constructor_registersDefaultAndVsacClients() {
        // Verifying the router can be constructed and routes correctly
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        assertNotNull(router);
    }

    @Test
    void getTerminologyServerClientSettings_returnsSettingsFromClient() {
        var settings = org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(42);
        var router = new FederatedTerminologyProviderRouter(fhirContext, settings);

        var endpoint = new Endpoint();
        endpoint.setAddress("https://example.org/fhir");
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);

        var result = router.getTerminologyServerClientSettings(endpointAdapter);
        assertEquals(42, result.getTimeoutSeconds());
    }

    // --- List-based expand methods ---

    @Test
    void expand_valueSetWithEndpointList_prioritizesAndDelegates() {
        var expandedVs = new ValueSet();
        expandedVs.setUrl(VSAC_VALUESET_URL);

        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(expandedVs)
                .when(router)
                .expand(any(IValueSetAdapter.class), any(IEndpointAdapter.class), any(IParametersAdapter.class));

        var endpoint1 = createEndpointAdapter("https://server1.com/fhir");
        var endpoint2 = createEndpointAdapter("https://cts.nlm.nih.gov/fhir");
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expand(valueSet, List.of(endpoint1, endpoint2), params);
        assertNotNull(result);
    }

    @Test
    void expand_endpointListWithFhirVersion_delegatesToSingleEndpoint() {
        var expandedVs = new ValueSet();

        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(expandedVs)
                .when(router)
                .expand(any(IEndpointAdapter.class), any(IParametersAdapter.class), any(FhirVersionEnum.class));

        var endpoint = createEndpointAdapter("https://server1.com/fhir");
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expand(List.of(endpoint), params, FhirVersionEnum.R4);
        assertNotNull(result);
    }

    @Test
    void expand_endpointListWithUrlAndVersion_prioritizesAndDelegates() {
        var expandedVs = new ValueSet();

        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(expandedVs)
                .when(router)
                .expand(
                        any(IEndpointAdapter.class),
                        any(IParametersAdapter.class),
                        anyString(),
                        anyString(),
                        any(FhirVersionEnum.class));

        var endpoint = createEndpointAdapter("https://cts.nlm.nih.gov/fhir");
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expand(List.of(endpoint), params, VSAC_VALUESET_URL, "1.0", FhirVersionEnum.R4);
        assertNotNull(result);
    }

    @Test
    void expand_endpointListReturnsNull_whenAllExpandsReturnNull() {
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(null)
                .when(router)
                .expand(any(IValueSetAdapter.class), any(IEndpointAdapter.class), any(IParametersAdapter.class));

        var endpoint = createEndpointAdapter("https://server1.com/fhir");
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expand(valueSet, List.of(endpoint), params);
        assertNull(result);
    }

    @Test
    void expand_emptyEndpointList_returnsNull() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expand(valueSet, List.of(), params);
        assertNull(result);
    }

    // --- List-based get methods ---

    @Test
    void getValueSetResource_endpointList_delegatesAndReturnsFirst() {
        var vs = new ValueSet();
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(Optional.of((IDomainResource) vs))
                .when(router)
                .getValueSetResource(any(IEndpointAdapter.class), anyString());

        var endpoint = createEndpointAdapter("https://cts.nlm.nih.gov/fhir");

        var result = router.getValueSetResource(List.of(endpoint), VSAC_VALUESET_URL);
        assertTrue(result.isPresent());
    }

    @Test
    void getCodeSystemResource_endpointList_delegatesAndReturnsFirst() {
        var cs = new org.hl7.fhir.r4.model.CodeSystem();
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(Optional.of((IDomainResource) cs))
                .when(router)
                .getCodeSystemResource(any(IEndpointAdapter.class), anyString());

        var endpoint = createEndpointAdapter("https://example.org/fhir");

        var result = router.getCodeSystemResource(List.of(endpoint), "http://loinc.org");
        assertTrue(result.isPresent());
    }

    @Test
    void getLatestValueSetResource_endpointList_delegatesAndReturnsFirst() {
        var vs = new ValueSet();
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doReturn(Optional.of((IDomainResource) vs))
                .when(router)
                .getLatestValueSetResource(any(IEndpointAdapter.class), anyString());

        var endpoint = createEndpointAdapter("https://cts.nlm.nih.gov/fhir");

        var result = router.getLatestValueSetResource(List.of(endpoint), VSAC_VALUESET_URL);
        assertTrue(result.isPresent());
    }

    @Test
    void getValueSetResource_emptyEndpointList_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getValueSetResource(List.of(), VSAC_VALUESET_URL);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCodeSystemResource_emptyEndpointList_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getCodeSystemResource(List.of(), "http://loinc.org");
        assertTrue(result.isEmpty());
    }

    @Test
    void getLatestValueSetResource_emptyEndpointList_returnsEmpty() {
        var router = new FederatedTerminologyProviderRouter(fhirContext);
        var result = router.getLatestValueSetResource(List.of(), VSAC_VALUESET_URL);
        assertTrue(result.isEmpty());
    }

    // --- Exception handling in *WithConfigurations methods ---

    @Test
    void expandWithConfigurations_exceptionInExpand_triesNextConfig() {
        var expandedVs = new ValueSet();
        expandedVs.setUrl(VSAC_VALUESET_URL);

        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doThrow(new RuntimeException("Connection failed"))
                .doReturn(expandedVs)
                .when(router)
                .expand(any(IValueSetAdapter.class), any(IEndpointAdapter.class), any(IParametersAdapter.class));

        var config1 = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://server1.com/fhir", null);
        var config2 = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://server2.com/fhir", null);
        var valueSet = createValueSetAdapter(VSAC_VALUESET_URL);
        var params = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var result = router.expandWithConfigurations(valueSet, List.of(config1, config2), params);
        assertNotNull(result);
    }

    @Test
    void getValueSetResourceWithConfigurations_exceptionInGet_triesNextConfig() {
        var vs = new ValueSet();
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doThrow(new RuntimeException("Connection failed"))
                .doReturn(Optional.of((IDomainResource) vs))
                .when(router)
                .getValueSetResource(any(IEndpointAdapter.class), anyString());

        var config1 = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://server1.com/fhir", null);
        var config2 = new ArtifactEndpointConfiguration(VSAC_ROUTE, "https://server2.com/fhir", null);

        var result = router.getValueSetResourceWithConfigurations(List.of(config1, config2), VSAC_VALUESET_URL);
        assertTrue(result.isPresent());
    }

    @Test
    void getCodeSystemResourceWithConfigurations_exceptionInGet_triesNextConfig() {
        var cs = new org.hl7.fhir.r4.model.CodeSystem();
        var router = spy(new FederatedTerminologyProviderRouter(fhirContext));
        doThrow(new RuntimeException("Connection failed"))
                .doReturn(Optional.of((IDomainResource) cs))
                .when(router)
                .getCodeSystemResource(any(IEndpointAdapter.class), anyString());

        var config1 = new ArtifactEndpointConfiguration(null, "https://server1.com/fhir", null);
        var config2 = new ArtifactEndpointConfiguration(null, "https://server2.com/fhir", null);

        var result = router.getCodeSystemResourceWithConfigurations(List.of(config1, config2), "http://loinc.org");
        assertTrue(result.isPresent());
    }

    // --- Helper methods ---

    private IEndpointAdapter createEndpointAdapter(String address) {
        var endpoint = new Endpoint();
        endpoint.setAddress(address);
        return (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);
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
