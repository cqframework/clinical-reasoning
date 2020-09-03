package org.opencds.cqf.cql.evaluator.builder.common;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class CommonModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory.class)
            .to(TerminologyProviderFactory.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory.class)
            .to(LibraryLoaderFactory.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.DataProviderFactory.class)
            .to(DataProviderFactory.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.DataProviderExtender.class)
            .to(DataProviderExtender.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.DataProviderConfigurer.class)
            .to(DataProviderConfigurer.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.EndpointConverter.class)
            .to(EndpointConverter.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory> modelFactoryBinder = 
            Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory.class);

        modelFactoryBinder.addBinding()
            .to(FhirModelResolverFactory.class);

    }
    
}