package org.opencds.cqf.cql.evaluator.builder.implementations.bundle;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.bundle.BundleLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

import ca.uhn.fhir.context.FhirContext;

public class BundleLibraryLoaderBuilderTests {
    private Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
    private TestUtils testUtils = new TestUtils();
    
    private LibraryLoader getBundleLibraryLoader(IBaseBundle libraries) {
        BundleLibraryLoaderBuilder bundleLibraryLoaderBuilder = new BundleLibraryLoaderBuilder();
        LibraryLoader libraryLoader = bundleLibraryLoaderBuilder.build(libraries, models, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
    }

    private IBaseBundle loadBundleLibraries(FhirContext fhirContext, String path) throws IOException {
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
    public void test_R4BundleLibraryLoader() throws IOException, URISyntaxException {
        FhirContext fhirContext = FhirContext.forR4();
        IBaseBundle libraries = loadBundleLibraries(fhirContext, "libraryresources/R4AllNecessaryLibraries.json");
        exceptionRule.expect(NotImplementedException.class);
        exceptionRule.expectMessage("Sorry there is no Bundle Library Loader implementation for anything newer or equal to R4 as of now.");
        LibraryLoader libraryLoader = getBundleLibraryLoader(libraries);
    }

    @Test
    public void test_DSTU3BundleLibraryLoader() throws IOException, URISyntaxException {
        FhirContext fhirContext = FhirContext.forDstu3();
        IBaseBundle libraries = loadBundleLibraries(fhirContext, "libraryresources/DSTU3AllNecessaryLibraries.json");
        LibraryLoader libraryLoader = getBundleLibraryLoader(libraries);
        assertThat(libraryLoader, instanceOf(TranslatingLibraryLoader.class));
    }
}