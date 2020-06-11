package org.opencds.cqf.cql.evaluator.builder.it.dstu3;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;
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
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;

public class DSTU3APIBundleIntegrationTests {
    private FhirContext fhirContext = FhirContext.forDstu3();
    private TestUtils testUtils = new TestUtils();

    private IBaseBundle loadBundle(FhirContext fhirContext, String path) throws IOException {
        IBaseBundle allNecessaryLibraries = testUtils.loadBundle(fhirContext, path);
        
        if (allNecessaryLibraries == null) {
            throw new IllegalArgumentException(String.format("Unable to read a resource from %s.", path));
        }

        Class<?> bundleClass = fhirContext.getResourceDefinition("Bundle").getImplementingClass();
        if (!bundleClass.equals(allNecessaryLibraries.getClass())) {
            throw new IllegalArgumentException(String.format("Resource at %s is not FHIR %s Bundle", path,
                    fhirContext.getVersion().getVersion().getFhirVersionString()));
        }

        IBaseBundle libraryDepsBundle = (IBaseBundle) allNecessaryLibraries;
        return libraryDepsBundle;
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Test
    public void test_DSTU3APIBundleTests() throws IOException {
        String primaryLibrary = "EXM104_FHIR3";
        String primaryLibraryVersion = "8.1.000";
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        versionedIdentifier.setId(primaryLibrary);
        versionedIdentifier.setVersion(primaryLibraryVersion);
        IBaseBundle terminologyBundle = this.loadBundle(fhirContext, "terminologyresources/DSTU3TerminologyBundle.json");
        IBaseBundle libraries = this.loadBundle(fhirContext, "libraryresources/DSTU3AllNecessaryLibraries.json");
        String dataPath = this.getClass().getClassLoader().getResource("dataresources/dstu3").getPath().replaceFirst("/", "");
        Map<String, String> modelUriMap = new HashMap<String, String>();
        modelUriMap.put("http://hl7.org/fhir", dataPath);
        Pair<String, Object> contextParameter = Pair.of("Patient", "denom-EXM104-FHIR3");
        CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
        cqlEvaluatorBuilder = cqlEvaluatorBuilder.withBundleLibraryLoader(libraries)
                .withBundleTerminologyProvider(terminologyBundle).withFileDataProvider(modelUriMap);
        CqlEvaluator cqlEvaluator = cqlEvaluatorBuilder.build(versionedIdentifier);
        assertThat(cqlEvaluatorBuilder.getTerminologyProvider(), instanceOf(BundleTerminologyProvider.class));
        assertThat(cqlEvaluatorBuilder.getLibraryLoader(), instanceOf(TranslatingLibraryLoader.class));
        assertThat(cqlEvaluatorBuilder.getDataProvider().get("http://hl7.org/fhir"), instanceOf(CompositeDataProvider.class));

        String model = "http://hl7.org/fhir";
        String version = "3.0.2";
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getLeft(), version), cqlEvaluatorBuilder.getModels().get(model).getLeft().equals(version));
        assertTrue(String.format("Model Map URIs did not match %s:%s", cqlEvaluatorBuilder.getModels().get(model).getRight(), "null"), cqlEvaluatorBuilder.getModels().get(model).getRight() == null);
    }
}