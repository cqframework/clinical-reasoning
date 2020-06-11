package org.opencds.cqf.cql.evaluator.builder.it.dstu3;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import static org.junit.Assert.assertTrue;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class DSTU3APIRemoteIntegrationTests {
    private ClientFactory getClientFactory(URL url, String version) throws IOException, InterruptedException, URISyntaxException {
        List<URL> urlList = new ArrayList<URL>();
        urlList.add(url);
        ClientFactory clientFactory = Mockito.mock(DefaultClientFactory.class);
        IGenericClient client = Mockito.mock(IGenericClient.class);
        Mockito.when(clientFactory.create(url)).thenReturn(client);
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);
        Mockito.when(client.getFhirContext()).thenReturn(versionEnum.newContext());
        return clientFactory;
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_DSTU3APIBundleTests() throws IOException, InterruptedException, URISyntaxException {
        String primaryLibrary = "EXM104_FHIR3";
        String primaryLibraryVersion = "8.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        URL url = new URL("http://localhost:8080/cqf-ruler-dstu3/fhir/");
        List<URL> urls = new ArrayList<URL>();
        urls.add(url);
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR3");
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        cqlEvaluatorBuilder.setClientFactory(getClientFactory(url, "3.0.2"));
        cqlEvaluatorBuilder = cqlEvaluatorBuilder.withRemoteLibraryLoader(url)
                .withRemoteTerminologyProvider(url).withRemoteDataProvider(urls);
        CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(Dstu3FhirTerminologyProvider.class));
        assertThat(cqlEvaluatorBuilder.getLibraryLoader(), instanceOf(TranslatingLibraryLoader.class));
        assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));

        String model = "http://hl7.org/fhir";
        String version = "3.0.2";
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getLeft(), version), cqlEvaluatorBuilder.getModels().get(model).getLeft().equals(version));
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getRight(), "null"), cqlEvaluatorBuilder.getModels().get(model).getRight() == null);
    }
}