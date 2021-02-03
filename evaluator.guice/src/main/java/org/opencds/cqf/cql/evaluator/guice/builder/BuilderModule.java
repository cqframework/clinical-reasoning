package org.opencds.cqf.cql.evaluator.guice.builder;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import org.opencds.cqf.cql.evaluator.builder.data.FhirFileRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.CqlFileLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirFileLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirFileTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory;

public class BuilderModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory.class)
            .to(TerminologyProviderFactory.class).in(Singleton.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory.class)
            .to(LibraryLoaderFactory.class).in(Singleton.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.DataProviderFactory.class)
            .to(DataProviderFactory.class).in(Singleton.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.EndpointConverter.class).in(Singleton.class);
        this.bind(org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer.class)
            .to(org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory> modelFactoryBinder = 
            Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory.class);

        modelFactoryBinder.addBinding()
            .to(FhirModelResolverFactory.class).in(Singleton.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory> retrieveFactoryBinder =
            Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory.class);

        retrieveFactoryBinder.addBinding().to(FhirFileRetrieveProviderFactory.class).in(Singleton.class);
        retrieveFactoryBinder.addBinding().to(FhirRestRetrieveProviderFactory.class).in(Singleton.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory> libraryContentProviderFactoryBinder =
        Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory.class);

        libraryContentProviderFactoryBinder.addBinding().to(CqlFileLibraryContentProviderFactory.class);
        libraryContentProviderFactoryBinder.addBinding().to(FhirFileLibraryContentProviderFactory.class);
        libraryContentProviderFactoryBinder.addBinding().to(FhirRestLibraryContentProviderFactory.class);

        Multibinder<org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory> terminologyProviderFactoryBinder =
            Multibinder.newSetBinder(binder(), org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory.class);
        
        terminologyProviderFactoryBinder.addBinding().to(FhirRestTerminologyProviderFactory.class);
        terminologyProviderFactoryBinder.addBinding().to(FhirFileTerminologyProviderFactory.class);
    }
    
}