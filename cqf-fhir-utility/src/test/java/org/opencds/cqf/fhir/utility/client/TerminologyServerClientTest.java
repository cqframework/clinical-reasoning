package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;

public class TerminologyServerClientTest {
    private static final String url = "www.test.com";
    private static final String version = "1.0";
    private static final String authoritativeSource = "www.source.com/ValueSet";
    private static final String username = "username";
    private static final String password = "password";
    private static final String urlParamName = "url";
    private static final String versionParamName = "valueSetVersion";

    @Test
    void testR4UrlAndVersion() {
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setUrl(url);
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
                .thenReturn(vs);

        var client = new TerminologyServerClient(contextMock);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.r4.model.Parameters(), username, password);
        assertNotNull(capt.getValue());
        var params = capt.getValue();
        assertEquals(
                url,
                ((org.hl7.fhir.r4.model.UriType)
                                params.getParameter(urlParamName).getValue())
                        .getValue());
        assertNull(params.getParameter(versionParamName));

        // when the valueset has a version it should be in params
        vs.setVersion(version);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.r4.model.Parameters(), username, password);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.r4.model.StringType)
                                params.getParameter(versionParamName).getValue())
                        .getValue());
    }

    @Test
    void testR5UrlAndVersion() {
        var vs = new org.hl7.fhir.r5.model.ValueSet();
        vs.setUrl(url);
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
                .thenReturn(vs);

        var client = new TerminologyServerClient(contextMock);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.r5.model.Parameters(), username, password);
        assertNotNull(capt.getValue());
        var params = capt.getValue();
        assertEquals(
                url,
                ((org.hl7.fhir.r5.model.UriType)
                                params.getParameter(urlParamName).getValue())
                        .getValue());
        assertNull(params.getParameter(versionParamName));

        // when the valueset has a version it should be in params
        vs.setVersion(version);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.r5.model.Parameters(), username, password);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.r5.model.StringType)
                                params.getParameter(versionParamName).getValue())
                        .getValue());
    }

    @Test
    void testDstu3UrlAndVersion() {
        var vs = new org.hl7.fhir.dstu3.model.ValueSet();
        vs.setUrl(url);
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
                .thenReturn(vs);

        var client = new TerminologyServerClient(contextMock);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.dstu3.model.Parameters(), username, password);
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
        vs.setVersion(version);
        client.expand(vs, authoritativeSource, new org.hl7.fhir.dstu3.model.Parameters(), username, password);
        params = capt.getAllValues().get(1);
        assertEquals(
                version,
                ((org.hl7.fhir.dstu3.model.StringType) params.getParameter().stream()
                                .filter(p -> p.getName().equals(versionParamName))
                                .findFirst()
                                .orElseThrow()
                                .getValue())
                        .getValue());
    }

    @Test
    void authoritativeSourceUrlParsing() {
        var supportedVersions = Arrays.asList(FhirVersionEnum.DSTU3, FhirVersionEnum.R4, FhirVersionEnum.R5);
        for (final var version : supportedVersions) {
            var ts = new TerminologyServerClient(new FhirContext(version));
            var theCorrectBaseServerUrl = "https://cts.nlm.nih.gov/fhir";
            // remove the FHIR type and the ID if included
            assertEquals(
                    theCorrectBaseServerUrl, ts.getAuthoritativeSourceBase(theCorrectBaseServerUrl + "/ValueSet/1"));
            // remove a FHIR type if one was included
            assertEquals(theCorrectBaseServerUrl, ts.getAuthoritativeSourceBase(theCorrectBaseServerUrl + "/ValueSet"));
            // don't break on the actual base url
            assertEquals(theCorrectBaseServerUrl, ts.getAuthoritativeSourceBase(theCorrectBaseServerUrl));
            // ensure it's forcing https
            assertEquals(
                    theCorrectBaseServerUrl,
                    ts.getAuthoritativeSourceBase(theCorrectBaseServerUrl.replace("https", "http")));
            // remove trailing slashes
            assertEquals(theCorrectBaseServerUrl, ts.getAuthoritativeSourceBase(theCorrectBaseServerUrl + "/"));
        }
    }
}
