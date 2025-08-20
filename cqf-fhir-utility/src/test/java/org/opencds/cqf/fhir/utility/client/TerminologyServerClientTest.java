package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.ExpandRunner.TerminologyServerExpansionException;

public class TerminologyServerClientTest {
    private static final String VALUE_SET = "ValueSet";
    private static final String EXPAND_OPERATION = "$expand";
    private static final String url = "www.test.com";
    private static final String version = "1.0";
    private static final String authoritativeSource = "www.source.com/ValueSet";
    private static final String username = "username";
    private static final String password = "password";
    private static final String urlParamName = TerminologyServerClient.urlParamName;
    private static final String versionParamName = TerminologyServerClient.versionParamName;

    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();

    @Test
    void testR4UrlAndVersion() {
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
        var valueSet = (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.r4.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r4.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r4.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r4.model.StringType(username)));
        endpoint.addExtension(
                new org.hl7.fhir.r4.model.Extension(Constants.APIKEY, new org.hl7.fhir.r4.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.r4.model.Parameters.class);

        var client = spy(new TerminologyServerClient(fhirContextR4));
        doReturn(valueSet.get()).when(client).expand(any(IGenericClient.class), any(), capt.capture());
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

        // when the ValueSet has a version it should be in params
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
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R5);
        var valueSet = (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.r5.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.r5.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.r5.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.r5.model.StringType(username)));
        endpoint.addExtension(
                new org.hl7.fhir.r5.model.Extension(Constants.APIKEY, new org.hl7.fhir.r5.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.r5.model.Parameters.class);

        var client = spy(new TerminologyServerClient(fhirContextR5));
        doReturn(valueSet.get()).when(client).expand(any(IGenericClient.class), any(), capt.capture());
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
        var factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.DSTU3);
        var valueSet =
                (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(new org.hl7.fhir.dstu3.model.ValueSet());
        valueSet.setUrl(url);
        var endpoint = factory.createEndpoint(new org.hl7.fhir.dstu3.model.Endpoint());
        endpoint.setAddress(authoritativeSource);
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.VSAC_USERNAME, new org.hl7.fhir.dstu3.model.StringType(username)));
        endpoint.addExtension(new org.hl7.fhir.dstu3.model.Extension(
                Constants.APIKEY, new org.hl7.fhir.dstu3.model.StringType(password)));
        var capt = ArgumentCaptor.forClass(org.hl7.fhir.dstu3.model.Parameters.class);

        var client = spy(new TerminologyServerClient(fhirContextDstu3));
        doReturn(valueSet.get()).when(client).expand(any(IGenericClient.class), any(), capt.capture());
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
        assertFalse(params.getParameter().stream().anyMatch(p -> p.getName().equals(versionParamName)));

        // when the ValueSet has a version it should be in params
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

    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"}) // Specify the enum values to test
    void addressUrlParsing(FhirVersionEnum supportedVersion) {
        var ctx = new FhirContext(supportedVersion);
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
                theCorrectBaseServerUrl, TerminologyServerClient.getAddressBase(theCorrectBaseServerUrl + "/", ctx));
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @EnumSource(
            value = FhirVersionEnum.class,
            names = {"DSTU3", "R4", "R5"}) // Specify the enum values to test
    void getLatestNonDraftSetsModifier(FhirVersionEnum supportedVersion) {
        // setup
        var contextMock = mock(FhirContext.class, new ReturnsDeepStubs());
        when(contextMock.getVersion().getVersion()).thenReturn(supportedVersion);
        var clientMock = mock(IGenericClient.class, new ReturnsDeepStubs());
        var endpointMock = mock(IEndpointAdapter.class, new ReturnsDeepStubs());
        when(endpointMock.getAddress()).thenReturn(authoritativeSource);
        when(contextMock.newRestfulGenericClient(any())).thenReturn(clientMock);
        ArgumentCaptor<Map<String, List<IQueryParameterType>>> urlParamsCaptor = ArgumentCaptor.forClass(Map.class);
        var whereMock = mock(IQuery.class);
        when(clientMock
                        .search()
                        .forResource(ArgumentMatchers.<Class<IBaseResource>>any())
                        .where(urlParamsCaptor.capture()))
                .thenReturn(whereMock);
        doReturn(BundleHelper.newBundle(supportedVersion)).when(whereMock).execute();

        // test
        var client = new TerminologyServerClient(contextMock);
        client.getLatestNonDraftValueSetResource(endpointMock, "www.test.com/fhir/ValueSet/123|1.2.3");
        var capturedUrlParams = urlParamsCaptor.getValue();
        var token = (TokenParam) capturedUrlParams.get("status").get(0);

        // assert
        assertEquals("draft", token.getValue());
        Assertions.assertSame(TokenParamModifier.NOT, token.getModifier());
    }

    @Test
    void expandRetrySuccessful() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress(baseUrl);
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        // ensure that the grouper is not expanded using the Tx Server
        var grouperUrl = "www.test.com/fhir/ValueSet/grouper";
        var grouper = new org.hl7.fhir.r4.model.ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new org.hl7.fhir.r4.model.CanonicalType(leafUrl));
        grouper.addExtension()
                .setUrl(Constants.AUTHORITATIVE_SOURCE_URL)
                .setValue(new org.hl7.fhir.r4.model.UriType(grouperUrl));
        var valueSetAdapter = (IValueSetAdapter) IAdapterFactory.createAdapterForResource(grouper);
        var parametersAdapter =
                (IParametersAdapter) IAdapterFactory.createAdapterForResource(new org.hl7.fhir.r4.model.Parameters());
        var leaf = new org.hl7.fhir.r4.model.ValueSet();
        leaf.setUrl(url);
        leaf.getExpansion().addContains().setSystem("system1").setCode("code1");
        leaf.getExpansion().addContains().setSystem("system2").setCode("code2");
        leaf.getExpansion().addContains().setSystem("system2").setCode("code3");
        leaf.getCompose().addInclude().setSystem("system1").addConcept().setCode("code1");
        var include2 = leaf.getCompose().addInclude().setSystem("system2");
        include2.addConcept().setCode("code2");
        include2.addConcept().setCode("code3");

        var fhirClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(fhirClient.getFhirContext().getVersion().getVersion()).thenReturn(FhirVersionEnum.R4);
        when(fhirClient
                        .operation()
                        .onType(eq(VALUE_SET))
                        .named(eq(EXPAND_OPERATION))
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any())
                        .execute())
                .thenThrow(new UnprocessableEntityException())
                .thenThrow(new UnprocessableEntityException())
                .thenReturn(leaf);
        // Important part - successful response on 3rd attempt to expand

        var client = spy(new TerminologyServerClient(fhirContextR4));
        doReturn(fhirClient).when(client).initializeClientWithAuth(any(IEndpointAdapter.class));

        var actual =
                (org.hl7.fhir.r4.model.ValueSet) client.expand(valueSetAdapter, endpointAdapter, parametersAdapter);

        assertEquals(3, actual.getExpansion().getContains().size());
        verify(client, never()).getValueSetResource(any(), any());
        verify(fhirClient, atLeast(3)).operation();
    }

    @Test
    void expandRetryFail() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new org.hl7.fhir.r4.model.Endpoint();
        endpoint.setAddress(baseUrl);
        var endpointAdapter = (IEndpointAdapter) IAdapterFactory.createAdapterForResource(endpoint);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        // ensure that the grouper is not expanded using the Tx Server
        var grouperUrl = "www.test.com/fhir/ValueSet/grouper";
        var grouper = new org.hl7.fhir.r4.model.ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new org.hl7.fhir.r4.model.CanonicalType(leafUrl));
        grouper.addExtension()
                .setUrl(Constants.AUTHORITATIVE_SOURCE_URL)
                .setValue(new org.hl7.fhir.r4.model.UriType(grouperUrl));
        var valueSetAdapter = (IValueSetAdapter) IAdapterFactory.createAdapterForResource(grouper);
        var parametersAdapter =
                (IParametersAdapter) IAdapterFactory.createAdapterForResource(new org.hl7.fhir.r4.model.Parameters());

        // important part - expand fails all 3 attempts
        var fhirClient = mock(IGenericClient.class, new ReturnsDeepStubs());
        when(fhirClient.getFhirContext().getVersion().getVersion()).thenReturn(FhirVersionEnum.R4);
        when(fhirClient
                        .operation()
                        .onType(eq(VALUE_SET))
                        .named(eq(EXPAND_OPERATION))
                        .withParameters(any(IBaseParameters.class))
                        .returnResourceType(any())
                        .execute())
                .thenThrow(new UnprocessableEntityException())
                .thenThrow(new UnprocessableEntityException())
                .thenThrow(new UnprocessableEntityException());

        var client = spy(new TerminologyServerClient(fhirContextR4));
        doReturn(fhirClient).when(client).initializeClientWithAuth(any(IEndpointAdapter.class));
        assertThrows(
                TerminologyServerExpansionException.class,
                () -> client.expand(valueSetAdapter, endpointAdapter, parametersAdapter));
        verify(client, never()).getValueSetResource(any(), any());
        verify(fhirClient, atLeast(3)).operation();
    }

    @Test
    void getValueSetResource_found() {
        var txServerClient = spy(new TerminologyServerClient(fhirContextR4));

        IEndpointAdapter endpoint = mock(IEndpointAdapter.class);
        IGenericClient client = mock(IGenericClient.class);
        IUntypedQuery<IBaseBundle> untyped = mock(IUntypedQuery.class);
        IQuery<IBaseBundle> query = mock(IQuery.class);

        // Fake ValueSet in a bundle
        var vs = new org.hl7.fhir.r4.model.ValueSet();
        vs.setId("123");
        vs.setUrl("http://example.org/vs/123");

        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setResource(vs);

        // Stub helpers
        doReturn(client).when(txServerClient).initializeClientWithAuth(endpoint);
        doReturn(org.hl7.fhir.r4.model.ValueSet.class).when(txServerClient).getValueSetClass();

        // Stub fluent chain
        when(client.search()).thenReturn(untyped);
        when(untyped.forResource(org.hl7.fhir.r4.model.ValueSet.class)).thenReturn(query);

        when(query.where(anyMap())).thenReturn(query);

        when(query.execute()).thenReturn(bundle);

        try (MockedStatic<IKnowledgeArtifactAdapter> mockedStatic =
                Mockito.mockStatic(IKnowledgeArtifactAdapter.class)) {

            mockedStatic
                    .when(() -> IKnowledgeArtifactAdapter.findLatestVersion(bundle))
                    .thenReturn(Optional.of((IDomainResource) vs));

            Optional<IDomainResource> result =
                    txServerClient.getValueSetResource(endpoint, "http://example.org/vs/123");

            assertTrue(result.isPresent());
            assertEquals("123", result.get().getIdElement().getIdPart());

            // Verify calls
            verify(client).search();
            verify(untyped).forResource(org.hl7.fhir.r4.model.ValueSet.class);
            verify(query).where(anyMap());
            verify(query).execute();
            mockedStatic.verify(() -> IKnowledgeArtifactAdapter.findLatestVersion(bundle));
        }
    }

    @Test
    void getCodeSystemClass_returnsR4ValueSet() {
        // Arrange
        var txServerClient = new TerminologyServerClient(fhirContextR4);

        // Act
        var returnedClass = txServerClient.getValueSetClass();

        // Assert
        assertNotNull(returnedClass);
        assertEquals(org.hl7.fhir.r4.model.ValueSet.class, returnedClass);
    }

    @Test
    void getCodeSystemClass_returnsR4CodeSystem() {
        // Arrange
        var txServerClient = new TerminologyServerClient(fhirContextR4);

        // Act
        var returnedClass = txServerClient.getCodeSystemClass();

        // Assert
        assertNotNull(returnedClass);
        assertEquals(org.hl7.fhir.r4.model.CodeSystem.class, returnedClass);
    }

    @Test
    void getCodeSystemResource_found() {
        var txServerClient = spy(new TerminologyServerClient(fhirContextR4));

        IEndpointAdapter endpoint = mock(IEndpointAdapter.class);
        IGenericClient client = mock(IGenericClient.class);
        IUntypedQuery<IBaseBundle> untyped = mock(IUntypedQuery.class);
        IQuery<IBaseBundle> query = mock(IQuery.class);

        // Fake CodeSystem in a bundle
        var cs = new org.hl7.fhir.r4.model.CodeSystem();
        cs.setId("123");
        cs.setUrl("http://example.org/vs/123");

        var bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setResource(cs);

        // Stub helpers
        doReturn(client).when(txServerClient).initializeClientWithAuth(endpoint);
        doReturn(org.hl7.fhir.r4.model.CodeSystem.class).when(txServerClient).getCodeSystemClass();

        // Stub fluent chain
        when(client.search()).thenReturn(untyped);
        when(untyped.forResource(org.hl7.fhir.r4.model.CodeSystem.class)).thenReturn(query);

        when(query.where(anyMap())).thenReturn(query);

        when(query.execute()).thenReturn(bundle);

        try (MockedStatic<IKnowledgeArtifactAdapter> mockedStatic =
                Mockito.mockStatic(IKnowledgeArtifactAdapter.class)) {

            mockedStatic
                    .when(() -> IKnowledgeArtifactAdapter.findLatestVersion(bundle))
                    .thenReturn(Optional.of((IDomainResource) cs));

            Optional<IDomainResource> result =
                    txServerClient.getCodeSystemResource(endpoint, "http://example.org/cs/123");

            assertTrue(result.isPresent());
            assertEquals("123", result.get().getIdElement().getIdPart());

            // Verify calls
            verify(client).search();
            verify(untyped).forResource(org.hl7.fhir.r4.model.CodeSystem.class);
            verify(query).where(anyMap());
            verify(query).execute();
            mockedStatic.verify(() -> IKnowledgeArtifactAdapter.findLatestVersion(bundle));
        }
    }
}
