package org.opencds.cqf.cql.evaluator.builder.implementations.remote;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteDataProviderBuilder;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class RemoteDataProviderBuilderTests {
    private Map<String, DataProvider> getRemoteDataProvider(String model, String version, URL url)
            throws IOException, InterruptedException, URISyntaxException {
        RemoteDataProviderBuilder remoteDataProviderBuilder = new RemoteDataProviderBuilder(null);
        List<URL> urlList = new ArrayList<URL>();
        urlList.add(url);
        ClientFactory clientFactory = Mockito.mock(DefaultClientFactory.class);
        IGenericClient client = Mockito.mock(IGenericClient.class);
        Mockito.when(clientFactory.create(url)).thenReturn(client);
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);
        Mockito.when(client.getFhirContext()).thenReturn(versionEnum.newContext());
        return remoteDataProviderBuilder.build(urlList, clientFactory);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R5RemoteDataProvider() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "4.2.0";
        URL terminologyURL = new URL("http://localhost:8080/cqf-ruler-r5/fhir/");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no Remote Data Provider implementation for anything newer than or equal to R5 as of now.");
        Map<String, DataProvider> r4DataProviderMap = getRemoteDataProvider(model, version, terminologyURL);
    }

    @Test
    public void test_R4RemoteDataProvider() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "4.0.1";
        URL terminologyURL = new URL("http://localhost:8080/cqf-ruler-r4/fhir/");
        Map<String, DataProvider> r4DataProviderMap = getRemoteDataProvider(model, version, terminologyURL);
        assertThat(r4DataProviderMap.get(model), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_DSTU3RemoteDataProvider() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "3.0.2";
        URL terminologyURL = new URL("http://localhost:8080/cqf-ruler-dstu3/fhir/");
        Map<String, DataProvider> r4DataProviderMap = getRemoteDataProvider(model, version, terminologyURL);
        assertThat(r4DataProviderMap.get(model), instanceOf(CompositeDataProvider.class));
    }

    @Test
    public void test_DSTU2RemoteDataProvider() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "1.0.2";
        URL terminologyURL = new URL("http://localhost:8080/cqf-ruler-dstu2/fhir/");
        Map<String, DataProvider> r4DataProviderMap = getRemoteDataProvider(model, version, terminologyURL);
        assertThat(r4DataProviderMap.get(model), instanceOf(CompositeDataProvider.class));
    }
}