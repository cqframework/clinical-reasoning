package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;

class VisitorHelperArtifactEndpointConfigurationTest {

    private static final String VSAC_ROUTE = "https://cts.nlm.nih.gov/fhir";
    private static final String EXAMPLE_ROUTE = "https://example.org/fhir";
    private static final String VSAC_ENDPOINT_URI = "https://cts.nlm.nih.gov/fhir";
    private static final String EXAMPLE_ENDPOINT_URI = "https://example.org/fhir/terminology";

    @Test
    void getArtifactEndpointConfigurations_singleConfigWithAllParts() {
        var params = new Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new UriType(VSAC_ROUTE));
        configParam.addPart().setName("endpointUri").setValue(new UriType(VSAC_ENDPOINT_URI));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        var config = configs.get(0);
        assertTrue(config.getArtifactRoute().isPresent());
        assertEquals(VSAC_ROUTE, config.getArtifactRoute().get());
        assertTrue(config.getEndpointUri().isPresent());
        assertEquals(VSAC_ENDPOINT_URI, config.getEndpointUri().get());
    }

    @Test
    void getArtifactEndpointConfigurations_multipleConfigs() {
        var params = new Parameters();

        // First config - VSAC
        var vsacConfig = params.addParameter().setName("artifactEndpointConfiguration");
        vsacConfig.addPart().setName("artifactRoute").setValue(new UriType(VSAC_ROUTE));
        vsacConfig.addPart().setName("endpointUri").setValue(new UriType(VSAC_ENDPOINT_URI));

        // Second config - Example
        var exampleConfig = params.addParameter().setName("artifactEndpointConfiguration");
        exampleConfig.addPart().setName("artifactRoute").setValue(new UriType(EXAMPLE_ROUTE));
        exampleConfig.addPart().setName("endpointUri").setValue(new UriType(EXAMPLE_ENDPOINT_URI));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(2, configs.size());

        // First config
        assertEquals(VSAC_ROUTE, configs.get(0).getArtifactRoute().orElse(null));
        assertEquals(VSAC_ENDPOINT_URI, configs.get(0).getEndpointUri().orElse(null));

        // Second config
        assertEquals(EXAMPLE_ROUTE, configs.get(1).getArtifactRoute().orElse(null));
        assertEquals(EXAMPLE_ENDPOINT_URI, configs.get(1).getEndpointUri().orElse(null));
    }

    @Test
    void getArtifactEndpointConfigurations_configWithEndpointResource() {
        var params = new Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new UriType(VSAC_ROUTE));

        var endpoint = new Endpoint();
        endpoint.setAddress(VSAC_ENDPOINT_URI);
        endpoint.setName("VSAC Terminology Server");
        configParam.addPart().setName("endpoint").setResource(endpoint);

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        var config = configs.get(0);
        assertTrue(config.getArtifactRoute().isPresent());
        assertEquals(VSAC_ROUTE, config.getArtifactRoute().get());
        assertTrue(config.getEndpoint().isPresent());
        assertEquals(VSAC_ENDPOINT_URI, config.getEndpoint().get().getAddress());
    }

    @Test
    void getArtifactEndpointConfigurations_configWithoutArtifactRoute_isFallback() {
        var params = new Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        // No artifactRoute - this is a fallback endpoint
        configParam.addPart().setName("endpointUri").setValue(new UriType(VSAC_ENDPOINT_URI));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        var config = configs.get(0);
        assertTrue(config.getArtifactRoute().isEmpty(), "No artifactRoute means fallback");
        assertTrue(config.getEndpointUri().isPresent());
    }

    @Test
    void getArtifactEndpointConfigurations_configWithoutEndpoint_isIgnored() {
        var params = new Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new UriType(VSAC_ROUTE));
        // No endpoint or endpointUri - config should be ignored

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertTrue(configs.isEmpty(), "Config without endpoint should be ignored");
    }

    @Test
    void getArtifactEndpointConfigurations_emptyParams_returnsEmptyList() {
        var params = new Parameters();

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void getArtifactEndpointConfigurations_nullParams_returnsEmptyList() {
        var configs = VisitorHelper.getArtifactEndpointConfigurations(null);

        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void getArtifactEndpointConfigurations_mixedWithOtherParams() {
        var params = new Parameters();

        // Other parameters that should be ignored
        params.addParameter().setName("terminologyEndpoint").setResource(new Endpoint());
        params.addParameter().setName("someOtherParam").setValue(new UriType("http://test.com"));

        // The artifact endpoint configuration
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new UriType(VSAC_ROUTE));
        configParam.addPart().setName("endpointUri").setValue(new UriType(VSAC_ENDPOINT_URI));

        // More other parameters
        params.addParameter().setName("anotherParam").setValue(new UriType("http://other.com"));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        assertEquals(VSAC_ROUTE, configs.get(0).getArtifactRoute().orElse(null));
    }

    @Test
    void getArtifactEndpointConfigurations_preservesOrderOfConfigs() {
        var params = new Parameters();

        // Add configs in specific order
        for (int i = 1; i <= 5; i++) {
            var configParam = params.addParameter().setName("artifactEndpointConfiguration");
            configParam.addPart().setName("artifactRoute").setValue(new UriType("https://server" + i + ".com"));
            configParam.addPart().setName("endpointUri").setValue(new UriType("https://endpoint" + i + ".com"));
        }

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(5, configs.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(
                    "https://server" + (i + 1) + ".com",
                    configs.get(i).getArtifactRoute().orElse(null));
        }
    }

    @Test
    void getArtifactEndpointConfigurations_dstu3Parameters() {
        var params = new org.hl7.fhir.dstu3.model.Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new org.hl7.fhir.dstu3.model.UriType(VSAC_ROUTE));
        configParam.addPart().setName("endpointUri").setValue(new org.hl7.fhir.dstu3.model.UriType(VSAC_ENDPOINT_URI));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        assertEquals(VSAC_ROUTE, configs.get(0).getArtifactRoute().orElse(null));
    }

    @Test
    void getArtifactEndpointConfigurations_r5Parameters() {
        var params = new org.hl7.fhir.r5.model.Parameters();
        var configParam = params.addParameter().setName("artifactEndpointConfiguration");
        configParam.addPart().setName("artifactRoute").setValue(new org.hl7.fhir.r5.model.UriType(VSAC_ROUTE));
        configParam.addPart().setName("endpointUri").setValue(new org.hl7.fhir.r5.model.UriType(VSAC_ENDPOINT_URI));

        var configs = VisitorHelper.getArtifactEndpointConfigurations(params);

        assertEquals(1, configs.size());
        assertEquals(VSAC_ROUTE, configs.get(0).getArtifactRoute().orElse(null));
    }
}
