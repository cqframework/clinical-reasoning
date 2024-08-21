package org.opencds.cqf.fhir.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClient;

public class ExpandHelperTest {
    private final AdapterFactory factory = AdapterFactory.forFhirVersion(FhirVersionEnum.R4);
    // we need to test that when expanding a grouper, we actually add child codes to the expansion.contains
    @Test
    void expandGrouperAddsLeafCodesToGrouperExpansionWithoutEndpointTest() {
        var leafUrl = "www.test.com/fhir/ValueSet/leaf";
        var grouper = new ValueSet();
        grouper.getCompose().getIncludeFirstRep().getValueSet().add(new CanonicalType(leafUrl));
        var leaf = createLeafWithUrl(leafUrl);

        // should be used
        var rep = mockRepositoryWithValueSetR4(leaf);

        // should not be used
        var client = mockTerminologyServerWithValueSetR4(leaf);

        var expandHelper = new ExpandHelper(rep.fhirContext(), client);
        expandHelper.expandValueSet(
                (ValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(new Parameters()),
                // important part of the test
                Optional.empty(),
                new ArrayList<ValueSetAdapter>(),
                new ArrayList<String>(),
                rep);
        assertEquals(3, grouper.getExpansion().getContains().size());
        verify(rep, times(1)).search(any(), any(), any(), any());
        verify(client, never()).getResource(any(), any(), any());
        verify(client, never()).expand(any(ValueSetAdapter.class), any(), any());
    }

    @Test
    void expandGrouperAddsLeafCodesToGrouperExpansionWithEndpointTest() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup Vsets
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

        var expandHelper = new ExpandHelper(rep.fhirContext(), client);
        expandHelper.expandValueSet(
                (ValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(new Parameters()),
                // important part of the test
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<ValueSetAdapter>(),
                new ArrayList<String>(),
                rep);
        assertEquals(3, grouper.getExpansion().getContains().size());
        verify(rep, never()).search(any(), any(), any());
        verify(client, times(1)).getResource(any(), any(), any());
        verify(client, times(1)).expand(any(ValueSetAdapter.class), any(), any());
    }

    @Test
    void expandGrouperUpdatesURLAndVersionExpParamsTest() {
        // setup tx server endpoint
        var baseUrl = "www.test.com/fhir";
        var endpoint = new Endpoint();
        endpoint.setAddress(baseUrl);
        // setup Vsets
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

        var expandHelper = new ExpandHelper(rep.fhirContext(), client);
        var expansionParams = new Parameters();
        // Setup exp params with Grouper URL and version
        expansionParams.addParameter(TerminologyServerClient.urlParamName, grouperUrl);
        expansionParams.addParameter(TerminologyServerClient.versionParamName, grouperVersion);
        expandHelper.expandValueSet(
                (ValueSetAdapter) this.factory.createKnowledgeArtifactAdapter(grouper),
                factory.createParameters(expansionParams),
                // important part of the test
                Optional.of(factory.createEndpoint(endpoint)),
                new ArrayList<ValueSetAdapter>(),
                new ArrayList<String>(),
                rep);
        var parametersCaptor = ArgumentCaptor.forClass(ParametersAdapter.class);
        verify(client, times(1)).expand(any(ValueSetAdapter.class), any(), parametersCaptor.capture());
        var childExpParams = parametersCaptor.getValue();
        assertNotNull(childExpParams.getParameter(TerminologyServerClient.urlParamName));
        // leaf is expanded with Leaf url not Grouper url
        assertTrue(((IPrimitiveType<String>) ((ParametersParameterComponent)
                                childExpParams.getParameter(TerminologyServerClient.urlParamName))
                        .getValue())
                .getValue()
                .equals(leafUrl));
        // the Version parameter is removed because leaf has no version
        assertNull(childExpParams.getParameter(TerminologyServerClient.versionParamName));
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

    Repository mockRepositoryWithValueSetR4(ValueSet valueSet) {
        var mockRepository = mock(Repository.class);
        when(mockRepository.fhirContext()).thenReturn(FhirContext.forR4Cached());
        org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry().setFullUrl(valueSet.getUrl()).setResource(valueSet);
        when(mockRepository.search(any(), any(), any(), any())).thenReturn(bundle);
        return mockRepository;
    }

    TerminologyServerClient mockTerminologyServerWithValueSetR4(ValueSet valueSet) {
        var mockClient = mock(TerminologyServerClient.class);
        when(mockClient.getResource(any(), anyString(), any())).thenReturn(java.util.Optional.of(valueSet));
        when(mockClient.expand(any(ValueSetAdapter.class), any(), any())).thenReturn(valueSet);
        return mockClient;
    }
}
