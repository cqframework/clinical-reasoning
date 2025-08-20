package org.opencds.cqf.fhir.cr.visitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class ExpandHelperTest {
    private final IAdapterFactory factory = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    // we need to test that when expanding a grouper, we actually add child codes to the expansion.contains
    @Test
    void expandGrouperAddsLeafCodesToGrouperExpansionWithoutEndpointTest() {
        var leafUrl = "www.test.com/fhir/ValueSet/leaf";
        var expansionDate = new Date();
        var grouper = new ValueSet();
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        var leaf = createLeafWithUrl(leafUrl);

        // should be used
        var rep = mockRepositoryWithValueSetR4(leaf);

        // should not be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep, client);
        expandHelper.expandValueSet(
                (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(new Parameters()),
                // important part of the test
                Optional.empty(),
                new ArrayList<IValueSetAdapter>(),
                new ArrayList<String>(),
                expansionDate);
        assertEquals(3, grouper.getExpansion().getContains().size());
        assertEquals(
                expansionDate.getTime(), grouper.getExpansion().getTimestamp().getTime());
        verify(rep, times(1)).search(any(), any(), any(Map.class), any());
        verify(client, never()).getValueSetResource(any(), any());
        verify(client, never()).expand(any(IValueSetAdapter.class), any(), any());
    }

    @Test
    void expandGrouperAddsLeafCodesToGrouperExpansionWithEndpointTest() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        // ensure that the grouper is not expanded using the Tx Server
        var grouperUrl = "www.different-base-url.com/fhir/ValueSet/grouper";
        var grouper = new ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        grouper.addExtension().setUrl(Constants.AUTHORITATIVE_SOURCE_URL).setValue(new UriType(grouperUrl));
        var leaf = createLeafWithUrl(leafUrl);

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(leaf);

        // should be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep, client);
        expandHelper.expandValueSet(
                (IValueSetAdapter) factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(new Parameters()),
                // important part of the test
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<IValueSetAdapter>(),
                new ArrayList<String>(),
                new Date());
        assertEquals(3, grouper.getExpansion().getContains().size());
        verify(rep, never()).search(any(), any(), any(Multimap.class));
        verify(client, times(1)).getValueSetResource(any(), any());
        verify(client, times(1)).expand(any(IValueSetAdapter.class), any(), any());
    }

    @Test
    void expandGrouperUpdatesURLAndVersionExpParamsTest() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        // ensure that the grouper is not expanded using the Tx Server
        var grouperUrl = "www.different-base-url.com/fhir/ValueSet/grouper";
        var grouperVersion = "1.9.2";
        var grouper = new ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.setVersion(grouperVersion);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        grouper.addExtension().setUrl(Constants.AUTHORITATIVE_SOURCE_URL).setValue(new UriType(grouperUrl));
        var leaf = createLeafWithUrl(leafUrl);

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(leaf);

        // should be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep, client);
        var expansionParams = new Parameters();
        // Setup exp params with Grouper URL and version
        expansionParams.addParameter(TerminologyServerClient.urlParamName, grouperUrl);
        expansionParams.addParameter(TerminologyServerClient.versionParamName, grouperVersion);
        expandHelper.expandValueSet(
                (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(expansionParams),
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<IValueSetAdapter>(),
                new ArrayList<String>(),
                new Date());
        var parametersCaptor = ArgumentCaptor.forClass(IParametersAdapter.class);
        verify(client, times(1)).expand(any(IValueSetAdapter.class), any(), parametersCaptor.capture());
        verify(rep, times(0)).search(any(), any(), any(Multimap.class), any());
        var childExpParams = parametersCaptor.getValue();
        assertNotNull(childExpParams.getParameter(TerminologyServerClient.urlParamName));
        // leaf is expanded with Leaf url not Grouper url

        var url = ((IPrimitiveType<String>)
                        (childExpParams.getParameter(TerminologyServerClient.urlParamName)).getValue())
                .getValue();
        assertEquals(leafUrl, url);
        // the Version parameter is removed because leaf has no version
        assertNull(childExpParams.getParameter(TerminologyServerClient.versionParamName));
    }

    @Test
    void expandingAnEmptyGrouperDoesNothing() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);

        var grouperUrl = "www.different-base-url.com/fhir/ValueSet/grouper";
        var grouperVersion = "1.9.2";
        var grouper = new ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.setVersion(grouperVersion);
        grouper.addExtension().setUrl(Constants.AUTHORITATIVE_SOURCE_URL).setValue(new UriType(grouperUrl));

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(new ValueSet());

        // shouldn't be used
        var client = mockTerminologyServerWithValueSetR4(new ValueSet());

        var expandHelper = new ExpandHelper(rep, client);
        var expansionParams = new Parameters();
        // Setup exp params with Grouper URL and version
        expansionParams.addParameter(TerminologyServerClient.urlParamName, grouperUrl);
        expansionParams.addParameter(TerminologyServerClient.versionParamName, grouperVersion);

        Exception notExpectingAnyException = null;
        try {
            expandHelper.expandValueSet(
                    (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                    factory.createParameters(expansionParams),
                    Optional.of(factory.createEndpoint(endpoint)),
                    new ArrayList<IValueSetAdapter>(),
                    new ArrayList<String>(),
                    new Date());
        } catch (Exception e) {
            notExpectingAnyException = e;
        }
        // should not error on empty Grouper
        assertNull(notExpectingAnyException);
        // should not call the client
        verify(client, times(0)).expand(any(IValueSetAdapter.class), any(), any());
        // should not search the repository
        verify(rep, times(0)).search(any(), any(), any(Multimap.class), any());
        // should not add any expansions
        assertEquals(0, grouper.getExpansion().getContains().size());
    }

    @Test
    void expandingAGrouperWhereChildHasNoExpansionDoesNotThrowError() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        var leaf = new ValueSet();
        leaf.setUrl(leafUrl);
        var grouperUrl = "www.different-base-url.com/fhir/ValueSet/grouper";
        var grouperVersion = "1.9.2";
        var grouper = new ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.setVersion(grouperVersion);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        grouper.addExtension().setUrl(Constants.AUTHORITATIVE_SOURCE_URL).setValue(new UriType(grouperUrl));

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(new ValueSet());

        // should be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep, client);
        var expansionParams = new Parameters();
        // Setup exp params with Grouper URL and version
        expansionParams.addParameter(TerminologyServerClient.urlParamName, grouperUrl);
        expansionParams.addParameter(TerminologyServerClient.versionParamName, grouperVersion);

        Exception notExpectingAnyException = null;
        try {
            expandHelper.expandValueSet(
                    (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                    factory.createParameters(expansionParams),
                    Optional.of(factory.createEndpoint(endpoint)),
                    new ArrayList<IValueSetAdapter>(),
                    new ArrayList<String>(),
                    new Date());
        } catch (Exception e) {
            notExpectingAnyException = e;
        }
        // should not error on empty leaf
        assertNull(notExpectingAnyException);
        // should call the client
        verify(client, times(1)).expand(any(IValueSetAdapter.class), any(), any());
        // should not search the repository
        verify(rep, times(0)).search(any(), any(), any(Multimap.class), any());
        // should not add any expansions
        assertEquals(0, grouper.getExpansion().getContains().size());
    }

    @Test
    void unsupportedParametersAreRemovedWhenExpanding() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup ValueSets
        var leafUrl = baseUrl + "/ValueSet/leaf";
        // ensure that the grouper is not expanded using the Tx Server
        var grouperUrl = "www.different-base-url.com/fhir/ValueSet/grouper";
        var grouperVersion = "1.9.2";
        var grouper = new ValueSet();
        grouper.setUrl(grouperUrl);
        grouper.setVersion(grouperVersion);
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        grouper.addExtension().setUrl(Constants.AUTHORITATIVE_SOURCE_URL).setValue(new UriType(grouperUrl));
        var leaf = createLeafWithUrl(leafUrl);
        leaf.setVersion("1.1.1");

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(leaf);

        // should be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep, client);
        var expansionParams = new Parameters();
        // Setup exp params with Grouper URL and version
        expansionParams.addParameter(TerminologyServerClient.urlParamName, grouperUrl);
        expansionParams.addParameter(TerminologyServerClient.versionParamName, grouperVersion);

        ExpandHelper.unsupportedParametersToRemove.forEach(unsupportedParam -> {
            expansionParams.addParameter(unsupportedParam, "test");
        });
        expandHelper.expandValueSet(
                (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(expansionParams),
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<IValueSetAdapter>(),
                new ArrayList<String>(),
                new Date());
        var parametersCaptor = ArgumentCaptor.forClass(IParametersAdapter.class);
        verify(client, times(1)).expand(any(IValueSetAdapter.class), any(), parametersCaptor.capture());
        var filteredExpansionParams = parametersCaptor.getValue();
        assertEquals(2, filteredExpansionParams.getParameter().size());
        ExpandHelper.unsupportedParametersToRemove.forEach(parameterUrl -> {
            assertNull(filteredExpansionParams.getParameter(parameterUrl));
        });
    }

    @Test
    void expandingAddsAVersion() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup ValueSets
        var url = baseUrl + "/ValueSet/leaf";
        var initiallyNoVersionNoExpansion = createLeafWithUrl(url);
        initiallyNoVersionNoExpansion.setExpansion(null);
        initiallyNoVersionNoExpansion.setVersion(null);
        var adapter = (IValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(initiallyNoVersionNoExpansion);
        var version = "1.2.3";
        var expandedValueSet = createLeafWithUrl(url);
        expandedValueSet.setVersion(version);

        // shouldn't be used
        var rep = mockRepositoryWithValueSetR4(new ValueSet());

        // should be used
        var client = mockTerminologyServerWithValueSetR4(expandedValueSet);

        var expandHelper = new ExpandHelper(rep, client);
        expandHelper.expandValueSet(
                adapter,
                factory.createParameters(new Parameters()),
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<IValueSetAdapter>(),
                new ArrayList<String>(),
                new Date());
        // leaf was expanded
        assertEquals(
                3, initiallyNoVersionNoExpansion.getExpansion().getContains().size());
        // leaf got a version
        assertEquals(version, initiallyNoVersionNoExpansion.getVersion());
        // trivial checks
        verify(rep, never()).search(any(), any(), any(Multimap.class));
        verify(client, times(1)).expand(eq(adapter), any(), any());
    }

    ValueSet createLeafWithUrl(String url) {
        var leaf = new ValueSet();
        leaf.setUrl(url);
        leaf.getExpansion().addContains().setSystem("system1").setCode("code1");
        leaf.getExpansion().addContains().setSystem("system2").setCode("code2");
        leaf.getExpansion().addContains().setSystem("system2").setCode("code3");
        leaf.getCompose().addInclude().setSystem("system1").addConcept().setCode("code1");
        var include2 = leaf.getCompose().addInclude().setSystem("system2");
        include2.addConcept().setCode("code2");
        include2.addConcept().setCode("code3");
        return leaf;
    }

    IRepository mockRepositoryWithValueSetR4(ValueSet valueSet) {
        var mockRepository = mock(IRepository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(Map.class), any())).thenReturn(bundle);
        return mockRepository;
    }

    TerminologyServerClient mockTerminologyServerWithValueSetR4(ValueSet valueSet) {
        var mockClient = mock(TerminologyServerClient.class);
        when(mockClient.getValueSetResource(any(), anyString())).thenReturn(java.util.Optional.of(valueSet));
        when(mockClient.expand(any(IValueSetAdapter.class), any(), any())).thenReturn(valueSet);
        return mockClient;
    }
}
