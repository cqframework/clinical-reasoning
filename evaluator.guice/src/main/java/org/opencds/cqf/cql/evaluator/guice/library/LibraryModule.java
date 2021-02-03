package org.opencds.cqf.cql.evaluator.guice.library;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;

import ca.uhn.fhir.context.FhirContext;

public class LibraryModule extends AbstractModule {

    @Provides
    @Singleton
    protected  FhirTypeConverter providesFhirTypeConverter(FhirContext fhirContext) {
        return new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    }
}