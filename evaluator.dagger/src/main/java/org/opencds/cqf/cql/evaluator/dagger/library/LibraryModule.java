package org.opencds.cqf.cql.evaluator.dagger.library;

import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;

import org.opencds.cqf.cql.evaluator.dagger.builder.BuilderModule;

import ca.uhn.fhir.context.FhirContext;


import dagger.Module;
import dagger.Provides;

@Module(includes = {BuilderModule.class})
public class LibraryModule {

    @Provides
    protected  FhirTypeConverter providesFhirTypeConverter(FhirContext fhirContext) {
        return new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    }
}