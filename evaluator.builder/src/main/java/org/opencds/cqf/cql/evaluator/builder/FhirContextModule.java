package org.opencds.cqf.cql.evaluator.builder;

import java.util.Objects;

import com.google.inject.AbstractModule;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class FhirContextModule extends AbstractModule {

    private FhirContext fhirContext;

    public FhirContextModule(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    public FhirContextModule(FhirVersionEnum fhirVersionEnum) {
        this.fhirContext = Objects
            .requireNonNull(fhirVersionEnum, "fhirVersionEnum can not be null")
            .newContext();
    }

    @Override
    protected void configure() {
        this.bind(FhirContext.class).toInstance(this.fhirContext);
    }
}