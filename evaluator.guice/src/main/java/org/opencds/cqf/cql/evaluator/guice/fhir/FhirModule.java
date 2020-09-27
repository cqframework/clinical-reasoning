package org.opencds.cqf.cql.evaluator.guice.fhir;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
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

    @Provides
    @Singleton
    protected org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory providesAdapterFactory(Map<FhirVersionEnum, AdapterFactory> registeredFactories) {
        return registeredFactories.get(this.fhirContext.getVersion().getVersion());
    }
}
