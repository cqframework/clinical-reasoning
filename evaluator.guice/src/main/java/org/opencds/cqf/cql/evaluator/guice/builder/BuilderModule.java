package org.opencds.cqf.cql.evaluator.guice.builder;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.opencds.cqf.cql.evaluator.builder.common.*;
import org.opencds.cqf.cql.evaluator.builder.data.BundleRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderExtender;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.CqlFileLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirFileLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;

public class BuilderModule extends AbstractModule {

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

        Multibinder<org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory> retrieveFactoryBinder =
            Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory.class);

        retrieveFactoryBinder.addBinding().to(BundleRetrieveProviderFactory.class);
        retrieveFactoryBinder.addBinding().to(FhirRestRetrieveProviderFactory.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory> librarySourceProviderFactoryBinder =
        Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.library.TypedLibrarySourceProviderFactory.class);

        librarySourceProviderFactoryBinder.addBinding().to(CqlFileLibrarySourceProviderFactory.class);
        librarySourceProviderFactoryBinder.addBinding().to(FhirFileLibrarySourceProviderFactory.class);
        librarySourceProviderFactoryBinder.addBinding().to(FhirRestLibrarySourceProviderFactory.class);
        
    }
    
}