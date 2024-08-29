package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;

public class TerminologyServerClientTest {
    private static final String url = "www.test.com";
    private static final String version = "1.0";
    private static final String authoritativeSource = "www.source.com/ValueSet";
    private static final String username = "username";
    private static final String password = "password";
    private static final String urlParamName = TerminologyServerClient.urlParamName;
    private static final String versionParamName = TerminologyServerClient.versionParamName;
    ;

    private FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void testR4UrlAndVersion() {
        var factory = AdapterFactory.forFhirVersion(FhirVersionEnum.R4);
        var valueSet = (ValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.r4.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r4.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r4.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r4.model.StringType(username)));
        endpoint.addExtension(
                new org.hl7.fhir.r4.model.Extension(Constants.APIKEY, new org.hl7.fhir.r4.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.r4.model.Parameters.class);
        var clientMock = mock(GenericClient.class, new ReturnsDeepStubs());
        var contextMock = mock(FhirContext.class, new ReturnsDeepStubs());
        when(contextMock.newRestfulGenericClient(any())).thenReturn(clientMock);
        when(contextMock.getVersion().getVersion()).thenReturn(FhirVersionEnum.R4);

        when(clientMock
                        .operation()
                        .onType(anyString())
                        .named(anyString())
                        .withParameters(capt.capture())
                        .returnResourceType(any())
                        .execute())
                .thenReturn(valueSet.get());
        var contextSpy = spy(fhirContextR4);
        doReturn(clientMock).when(contextSpy).newRestfulGenericClient(any());

        var client = new TerminologyServerClient(contextSpy);
        var parameters = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());
        client.expand(valueSet, endpoint, parameters);
        assertNotNull(capt.getValue());
        var params = capt.getValue();
        assertEquals(
                url,
                ((org.hl7.fhir.r4.model.UriType)
                                params.getParameter(urlParamName).getValue())
                        .getValue());
        assertNull(params.getParameter(versionParamName));

        // when the valueset has a version it should be in params
        valueSet.setVersion(version);
        client.expand(valueSet, endpoint, parameters);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.r4.model.StringType)
                                params.getParameter(versionParamName).getValue())
                        .getValue());

        // if url is provided we don't need to pass in a ValueSet
        var urlAndVersionParams = factory.createParameters(new org.hl7.fhir.r4.model.Parameters());
        urlAndVersionParams.addParameter(urlParamName, new org.hl7.fhir.r4.model.UrlType(url));
        urlAndVersionParams.addParameter(versionParamName, new org.hl7.fhir.r4.model.StringType(version));
        Exception noException = null;
        try {
            var expanded = client.expand(endpoint, urlAndVersionParams, FhirVersionEnum.R4);
            assertEquals(expanded.getClass(), valueSet.get().getClass());
        } catch (Exception e) {
            noException = e;
        }
        assertNull(noException);
    }

    @Test
    void testR5UrlAndVersion() {
        var factory = AdapterFactory.forFhirVersion(FhirVersionEnum.R5);
        var valueSet = (ValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.r5.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r5.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r5.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r5.model.StringType(username)));
        endpoint.addExtension(
                new org.hl7.fhir.r5.model.Extension(Constants.APIKEY, new org.hl7.fhir.r5.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.r5.model.Parameters.class);
        var clientMock = mock(GenericClient.class, new ReturnsDeepStubs());
        var contextMock = mock(FhirContext.class, new ReturnsDeepStubs());
        when(contextMock.newRestfulGenericClient(any())).thenReturn(clientMock);
        when(contextMock.getVersion().getVersion()).thenReturn(FhirVersionEnum.R5);
        when(clientMock
                        .operation()
                        .onType(anyString())
                        .named(anyString())
                        .withParameters(capt.capture())
                        .returnResourceType(any())
                        .execute())
                .thenReturn(valueSet.get());
        var contextSpy = spy(fhirContextR5);
        doReturn(clientMock).when(contextSpy).newRestfulGenericClient(any());

        var client = new TerminologyServerClient(contextSpy);
        var parameters = factory.createParameters(new org.hl7.fhir.r5.model.Parameters());
        client.expand(valueSet, endpoint, parameters);
        assertNotNull(capt.getValue());
        var params = capt.getValue();
        assertEquals(
                url,
                ((org.hl7.fhir.r5.model.UriType)
                                params.getParameter(urlParamName).getValue())
                        .getValue());
        assertNull(params.getParameter(versionParamName));

        // when the valueset has a version it should be in params
        valueSet.setVersion(version);
        client.expand(valueSet, endpoint, parameters);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.r5.model.StringType)
                                params.getParameter(versionParamName).getValue())
                        .getValue());

        // if url is provided we don't need to pass in a ValueSet
        var urlAndVersionParams = factory.createParameters(new org.hl7.fhir.r5.model.Parameters());
        urlAndVersionParams.addParameter(urlParamName, new org.hl7.fhir.r5.model.UrlType(url));
        urlAndVersionParams.addParameter(versionParamName, new org.hl7.fhir.r5.model.StringType(version));
        Exception noException = null;
        try {
            var expanded = client.expand(endpoint, urlAndVersionParams, FhirVersionEnum.R5);
            assertEquals(expanded.getClass(), valueSet.get().getClass());
        } catch (Exception e) {
            noException = e;
        }
        assertNull(noException);
    }

    @Test
    void testDstu3UrlAndVersion() {
        var factory = AdapterFactory.forFhirVersion(FhirVersionEnum.DSTU3);
        var valueSet =
                (ValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.dstu3.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.dstu3.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.dstu3.model.StringType(username)));
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.APIKEY, new org.hl7.fhir.dstu3.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.dstu3.model.Parameters.class);
        var clientMock = mock(GenericClient.class, new ReturnsDeepStubs());
        var contextMock = mock(FhirContext.class, new ReturnsDeepStubs());
        when(contextMock.newRestfulGenericClient(any())).thenReturn(clientMock);
        when(contextMock.getVersion().getVersion()).thenReturn(FhirVersionEnum.DSTU3);
        when(clientMock
                        .operation()
                        .onType(anyString())
                        .named(anyString())
                        .withParameters(capt.capture())
                        .returnResourceType(any())
                        .execute())
                .thenReturn(valueSet.get());
        var contextSpy = spy(fhirContextDstu3);
        doReturn(clientMock).when(contextSpy).newRestfulGenericClient(any());

        var client = new TerminologyServerClient(contextSpy);
        var parameters = factory.createParameters(new org.hl7.fhir.dstu3.model.Parameters());
        client.expand(valueSet, endpoint, parameters);
        assertNotNull(capt.getValue());
        var params = capt.getValue();
        assertEquals(
                url,
                ((org.hl7.fhir.dstu3.model.UriType) params.getParameter().stream()
                                .filter(p -> p.getName().equals(urlParamName))
                                .findAny()
                                .orElseThrow()
                                .getValue())
                        .getValue());
        assertTrue(!params.getParameter().stream().anyMatch(p -> p.getName().equals(versionParamName)));

        // when the valueset has a version it should be in params
        valueSet.setVersion(version);
        client.expand(valueSet, endpoint, parameters);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.dstu3.model.StringType) params.getParameter().stream()
                                .filter(p -> p.getName().equals(versionParamName))
                                .findFirst()
                                .orElseThrow()
                                .getValue())
                        .getValue());

        // if url is provided we don't need to pass in a ValueSet
        var urlAndVersionParams = factory.createParameters(new org.hl7.fhir.dstu3.model.Parameters());
        urlAndVersionParams.addParameter(urlParamName, new org.hl7.fhir.dstu3.model.UriType(url));
        urlAndVersionParams.addParameter(versionParamName, new org.hl7.fhir.dstu3.model.StringType(version));
        Exception noException = null;
        try {
            var expanded = client.expand(endpoint, urlAndVersionParams, FhirVersionEnum.DSTU3);
            assertEquals(expanded.getClass(), valueSet.get().getClass());
        } catch (Exception e) {
            noException = e;
        }
        assertNull(noException);
    }

    @Test
    void addressUrlParsing() {
        var supportedVersions = Arrays.asList(FhirVersionEnum.DSTU3, FhirVersionEnum.R4, FhirVersionEnum.R5);
        for (final var version : supportedVersions) {
            var ctx = new FhirContext(version);
            var theCorrectBaseServerUrl = "https://cts.nlm.nih.gov/fhir";
            // remove the FHIR type and the ID if included
            assertEquals(
                    theCorrectBaseServerUrl,
                    TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl + "/ValueSet/1", ctx));
            // remove a FHIR type if one was included
            assertEquals(
                    theCorrectBaseServerUrl,
                    TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl + "/ValueSet", ctx));
            // don't break on the actual base url
            assertEquals(theCorrectBaseServerUrl, TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl, ctx));
            // ensure it's forcing https
            assertEquals(
                    theCorrectBaseServerUrl,
                    TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl.replace("https", "http"), ctx));
            // remove trailing slashes
            assertEquals(
                    theCorrectBaseServerUrl,
                    TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl + "/", ctx));
        }
    }
}
