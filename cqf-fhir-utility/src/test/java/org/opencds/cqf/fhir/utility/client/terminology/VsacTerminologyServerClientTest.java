package org.opencds.cqf.fhir.utility.client.terminology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

class VsacTerminologyServerClientTest {

    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void isCanonicalMatch_vsacHttpsUrl_returnsTrue() {
        var client = new VsacTerminologyServerClient(fhirContextR4);
        assertTrue(client.isCanonicalMatch("https://cts.nlm.nih.gov/fhir"));
    }

    @Test
    void isCanonicalMatch_vsacHttpUrl_returnsTrue() {
        var client = new VsacTerminologyServerClient(fhirContextR4);
        assertTrue(client.isCanonicalMatch("http://cts.nlm.nih.gov/fhir"));
    }

    @Test
    void isCanonicalMatch_vsacUrlWithPath_returnsTrue() {
        var client = new VsacTerminologyServerClient(fhirContextR4);
        assertTrue(client.isCanonicalMatch(
                "https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001"));
    }

    @Test
    void isCanonicalMatch_nonVsacUrl_returnsFalse() {
        var client = new VsacTerminologyServerClient(fhirContextR4);
        assertFalse(client.isCanonicalMatch("https://example.org/fhir"));
    }

    @Test
    void isCanonicalMatch_genericFhirUrl_returnsFalse() {
        var client = new VsacTerminologyServerClient(fhirContextR4);
        assertFalse(client.isCanonicalMatch("https://tx.fhir.org/r4"));
    }

    @Test
    void initializeClientWithAuth_withVsacExtensions_registersAuth() {
        var endpoint = new Endpoint();
        endpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        endpoint.addExtension(new Extension(Constants.VSAC_USERNAME, new StringType("testuser")));
        endpoint.addExtension(new Extension(Constants.APIKEY, new StringType("testapikey")));
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);

        var client = new VsacTerminologyServerClient(fhirContextR4);
        var fhirClient = client.initializeClientWithAuth(endpointAdapter);

        assertNotNull(fhirClient);
    }

    @Test
    void initializeClientWithAuth_withoutVsacExtensions_skipsAuth() {
        var endpoint = new Endpoint();
        endpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);

        var client = new VsacTerminologyServerClient(fhirContextR4);
        var fhirClient = client.initializeClientWithAuth(endpointAdapter);

        assertNotNull(fhirClient);
    }

    @Test
    void expand_usesVsacClient() {
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
        var valueSet = (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.r4.model.ValueSet());
        valueSet.setUrl("https://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001");

        var endpoint = new Endpoint();
        endpoint.setAddress("https://cts.nlm.nih.gov/fhir");
        endpoint.addExtension(new Extension(Constants.VSAC_USERNAME, new StringType("testuser")));
        endpoint.addExtension(new Extension(Constants.APIKEY, new StringType("testapikey")));
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);
        var parametersAdapter = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());

        var expandedVs = new org.hl7.fhir.r4.model.ValueSet();
        expandedVs.getExpansion().addContains().setSystem("http://loinc.org").setCode("12345-6");

        var client = spy(new VsacTerminologyServerClient(fhirContextR4));
        doReturn(expandedVs).when(client).expand(any(IGenericClient.class), anyString(), any(IBaseParameters.class));

        var result = (org.hl7.fhir.r4.model.ValueSet) client.expand(valueSet, endpointAdapter, parametersAdapter);

        assertEquals(1, result.getExpansion().getContains().size());
    }

    @Test
    void constructor_withSettings_usesProvidedSettings() {
        var settings = org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings.getDefault()
                .setTimeoutSeconds(10);
        var client = new VsacTerminologyServerClient(fhirContextR4, settings);
        assertEquals(10, client.getTerminologyServerClientSettings().getTimeoutSeconds());
    }
}
