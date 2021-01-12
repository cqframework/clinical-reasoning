package org.opencds.cqf.cql.evaluator.guice.fhir;

import static java.util.Objects.requireNonNull;

import com.google.inject.AbstractModule;

import org.opencds.cqf.cql.evaluator.guice.fhir.adapter.AdapterModule;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

public class FhirModule extends AbstractModule {

    private FhirContext fhirContext;
    public FhirModule(FhirVersionEnum fhirVersionEnum) {
        this(fhirVersionEnum.newContext());
    }

    public FhirModule(FhirContext fhirContext)  {
        this.fhirContext = requireNonNull(fhirContext, "fhirContext can not be null.");
    }

    @Override
    protected void configure() {
        this.bind(FhirContext.class).toInstance(this.fhirContext);
        this.install(new AdapterModule());
    }
}
