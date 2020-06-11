package org.opencds.cqf.cql.evaluator.builder.implementations.string;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Test;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.evaluator.builder.TestUtils;
import org.opencds.cqf.cql.evaluator.builder.implementation.string.StringLibraryLoaderBuilder;
import org.opencds.cqf.cql.evaluator.execution.loader.TranslatingLibraryLoader;

import ca.uhn.fhir.context.FhirContext;

public class StringLibraryLoaderBuilderTests {
    
    private Map<String, Pair<String, String>> models = new HashMap<String, Pair<String, String>>();
    
    private LibraryLoader getStringLibraryLoader(List<String> libraries) {
        StringLibraryLoaderBuilder stringLibraryLoaderBuilder = new StringLibraryLoaderBuilder();
        LibraryLoader libraryLoader = stringLibraryLoaderBuilder.build(libraries, models, CqlTranslatorOptions.defaultOptions());
        return libraryLoader;
    }

    private List<String> loadStringLibraries() throws IOException {
        TestUtils testUtils = new TestUtils();
        FhirContext fhirContext = FhirContext.forR4();
        List<String> libraries = new ArrayList<String>();
        String primaryLibrary = testUtils.loadString("libraryresources/PrimaryLibrary.json");
        libraries.add(primaryLibrary);
        
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
        return libraries;
    }

    @Test
    public void test_R4StringLibraryLoader() throws IOException, URISyntaxException {
        List<String> libraries = loadStringLibraries();
        LibraryLoader libraryLoader = getStringLibraryLoader(libraries);
        assertThat(libraryLoader, instanceOf(TranslatingLibraryLoader.class));
    }
}