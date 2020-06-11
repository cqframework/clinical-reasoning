package org.opencds.cqf.cql.evaluator.builder.it.r4;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencds.cqf.cql.evaluator.execution.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderTerminologyContext;
import org.apache.commons.lang3.NotImplementedException;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

import ca.uhn.fhir.context.FhirContext;

public class R4APIStringIntegrationTests {
    private TestUtils testUtils = new TestUtils();
    
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_R4APIStringTests() throws IOException {
        String primaryLibraryName = "EXM104";
        String primaryLibraryVersion = "9.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibraryName);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        FhirContext fhirContext = FhirContext.forR4();
        List<String> libraries = new ArrayList<String>();
        String primaryLibraryResource = testUtils.loadString("libraryresources/PrimaryLibrary.json");
        IBaseResource resource = testUtils.loadBundle(fhirContext, "libraryresources/LibraryDepsBundle.json");
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", "libraryresources/LibraryDepsBundle.json"));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(resource.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", "libraryresources/LibraryDepsBundle.json",
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        Bundle libraryDepsBundle = (Bundle) resource;
        libraryDepsBundle.getEntry()
            .forEach(entry -> 
                libraries.add(fhirContext.newJsonParser().encodeResourceToString(entry.getResource()))
            );
        List<String> terminologyBundles = new ArrayList<String>();
        String terminologyBundle = testUtils.loadString("terminologyresources/TerminologyBundle.json");
        terminologyBundles.add(terminologyBundle);
        List<String> dataBundles = new ArrayList<String>();
        String dataBundle = testUtils.loadString("dataresources/DataBundle.json");
        dataBundles.add(dataBundle);
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR4");
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        BuilderTerminologyContext builderTerminologyContext = cqlEvaluatorBuilder.withLibraryLoader(libraries);
        assertThat(cqlEvaluatorBuilder.getLibraryLoader(), instanceOf(TranslatingLibraryLoader.class));
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("String Representations of Terminology Bundles is not yet supported.");
        // cqlEvaluatorBuilder = builderTerminologyContext.withStringTerminologyProvider(terminologyBundles).withStringDataProvider(dataBundles);
        // CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        // assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        // assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));

        // String model = "http://hl7.org/fhir";
        // String version = "4.0.1";
        // assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getLeft(), version), cqlEvaluatorBuilder.getModels().get(model).getLeft().equals(version));
        // assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getRight(), "null"), cqlEvaluatorBuilder.getModels().get(model).getRight() == null);
    }
}