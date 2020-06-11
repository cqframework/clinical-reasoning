package org.opencds.cqf.cql.evaluator.builder.implementations.remote;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
public class RemoteLibraryLoaderBuilderTests {
    private Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();

    private LibraryLoader getRemoteLibraryLoader(String model, String version, URL url)
            throws IOException, InterruptedException, URISyntaxException {
        RemoteLibraryLoaderBuilder remoteLibraryLoaderBuilder = new RemoteLibraryLoaderBuilder();
        ClientFactory clientFactory = Mockito.mock(DefaultClientFactory.class);
        IGenericClient client = Mockito.mock(IGenericClient.class);
        Mockito.when(clientFactory.create(url)).thenReturn(client);
        FhirVersionEnum versionEnum = ModelVersionHelper.forVersionString(version);
        Mockito.when(client.getFhirContext()).thenReturn(versionEnum.newContext());
        return remoteLibraryLoaderBuilder.build(url, models, clientFactory);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R5RemoteLibraryLoader() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "4.2.0";
        URL libraryURL = new URL("http://localhost:8080/cqf-ruler-r5/fhir/");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no implementation for anything newer than or equal to R4 as of now.");
        LibraryLoader r4LibraryLoader = getRemoteLibraryLoader(model, version, libraryURL);
    }

    @Test
    public void test_R4RemoteLibraryLoader() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "4.0.1";
        URL libraryURL = new URL("http://localhost:8080/cqf-ruler-r4/fhir/");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no implementation for anything newer than or equal to R4 as of now.");
        LibraryLoader r4LibraryLoader = getRemoteLibraryLoader(model, version, libraryURL);
    }

    @Test
    public void test_DSTU3RemoteLibraryLoader() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "3.0.2";
        URL libraryURL = new URL("http://localhost:8080/cqf-ruler-dstu3/fhir/");
        LibraryLoader r4LibraryLoader = getRemoteLibraryLoader(model, version, libraryURL);
        assertThat(r4LibraryLoader, instanceOf(TranslatingLibraryLoader.class));
    }

    @Test
    public void test_DSTU2RemoteLibraryLoader() throws IOException, InterruptedException, URISyntaxException {
        String model = "http://hl7.org/fhir";
        String version = "1.0.2";
        URL libraryURL = new URL("http://localhost:8080/cqf-ruler-dstu2/fhir/");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no implementation for anything older than DSTU3 as of now.");
        LibraryLoader r4LibraryLoader = getRemoteLibraryLoader(model, version, libraryURL);
    }
}