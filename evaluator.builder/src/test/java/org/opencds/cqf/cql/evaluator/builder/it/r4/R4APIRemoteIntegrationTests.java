package org.opencds.cqf.cql.evaluator.builder.it.r4;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.factory.ClientFactory;
import org.opencds.cqf.cql.evaluator.builder.factory.DefaultClientFactory;
import org.opencds.cqf.cql.evaluator.builder.helper.ModelVersionHelper;
import org.opencds.cqf.cql.evaluator.builder.implementation.remote.RemoteTerminologyProviderBuilder;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderDataContext;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderTerminologyContext;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class R4APIRemoteIntegrationTests {
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
    public void test_R4APIBundleTests() throws IOException, InterruptedException, URISyntaxException {
        String primaryLibrary = "EXM104";
        String primaryLibraryVersion = "9.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        // Can I Mock this?
        // Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
        // models.put(model, Pair.of(version, null));
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        URL url = new URL("http://localhost:8080/cqf-ruler-r4/fhir/");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no implementation for anything newer than or equal to R4 as of now.");
        cqlEvaluatorBuilder.setClientFactory(getClientFactory(url, "4.0.1"));
        BuilderTerminologyContext builderTerminologyContext = cqlEvaluatorBuilder.withRemoteLibraryLoader(url);
        // exceptionRule.expect(NotImplementedException.class);
        // exceptionRule.expectMessage("Sorry there is no implementation for anything newer than or equal to R5 as of now.");
        // BuilderDataContext builderDataContext = builderTerminologyContext.withRemoteTerminologyProvider(url);
        // cqlEvaluatorBuilder = builderDataContext.withFileDataProvider(modelUriMap);
        // CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        // assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        // assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));
    }
}