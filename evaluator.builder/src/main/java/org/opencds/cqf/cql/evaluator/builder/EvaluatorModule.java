package org.opencds.cqf.cql.evaluator.builder;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.opencds.cqf.cql.evaluator.builder.api.DataProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.api.DataProviderExtender;
import org.opencds.cqf.cql.evaluator.builder.api.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.api.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.api.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.api.TerminologyProviderFactory;

public class EvaluatorModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(TerminologyProviderFactory.class)
            .to(org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory.class);
        this.bind(LibraryLoaderFactory.class)
            .to(org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory.class);
        this.bind(DataProviderFactory.class)
            .to(org.opencds.cqf.cql.evaluator.builder.DataProviderFactory.class);
        this.bind(DataProviderExtender.class)
            .to(org.opencds.cqf.cql.evaluator.builder.DataProviderExtender.class);
        this.bind(DataProviderConfigurer.class)
            .to(org.opencds.cqf.cql.evaluator.builder.DataProviderConfigurer.class);

        Multibinder<ModelResolverFactory> modelFactoryBinder = 
            Multibinder.newSetBinder(binder(), ModelResolverFactory.class);

        modelFactoryBinder.addBinding()
            .to(org.opencds.cqf.cql.evaluator.builder.FhirModelResolverFactory.class);

    }
    
}