package org.opencds.cqf.cql.evaluator.guice.library;

import static org.testng.Assert.assertNotNull;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;
import org.opencds.cqf.cql.evaluator.library.LibraryProcessor;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirVersionEnum;

public class LibraryModuleTests {

    @Test
    public void canInstantiateDstu3() {
        Injector injector = Guice.createInjector(
            new FhirModule(FhirVersionEnum.DSTU3), 
            new BuilderModule(),
            new LibraryModule());

        LibraryProcessor libraryProcessor = injector.getInstance(LibraryProcessor.class);

        assertNotNull(libraryProcessor);
    }

    @Test
    public void canInstantiateR4() {
        Injector injector = Guice.createInjector(
            new FhirModule(FhirVersionEnum.R4), 
            new BuilderModule(),
            new LibraryModule());

        LibraryProcessor libraryProcessor = injector.getInstance(LibraryProcessor.class);

        assertNotNull(libraryProcessor);
    }
}