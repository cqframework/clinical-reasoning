package org.opencds.cqf.cql.evaluator.dagger.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.data.FhirFileRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.FhirRestRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.CqlFileLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirFileLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirRestLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirFileTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.FhirRestTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.dagger.fhir.adapter.AdapterModule;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
// import dagger.Provides;
import dagger.multibindings.IntoSet;

@Module(includes = { BuilderBindingModule.class, AdapterModule.class })
public class BuilderModule {

    // @Override
    // protected void configure() {
    // this.bind(org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory.class)
    // .to(TerminologyProviderFactory.class).in(Singleton.class);
    // this.bind(org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory.class)
    // .to(LibraryLoaderFactory.class).in(Singleton.class);
    // this.bind(org.opencds.cqf.cql.evaluator.builder.DataProviderFactory.class)
    // .to(DataProviderFactory.class).in(Singleton.class);
    // this.bind(org.opencds.cqf.cql.evaluator.builder.EndpointConverter.class).in(Singleton.class);
    // this.bind(org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer.class)
    // .to(org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer.class);

    // Multibinder<org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory>
    // modelFactoryBinder =
    // Multibinder.newSetBinder(binder(),
    // org.opencds.cqf.cql.evaluator.builder.ModelResolverFactory.class);

    // modelFactoryBinder.addBinding()
    // .to(FhirModelResolverFactory.class).in(Singleton.class);

    // Multibinder<org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory>
    // retrieveFactoryBinder =
    // Multibinder.newSetBinder(binder(),
    // org.opencds.cqf.cql.evaluator.builder.data.TypedRetrieveProviderFactory.class);

    // retrieveFactoryBinder.addBinding().to(FhirFileRetrieveProviderFactory.class).in(Singleton.class);
    // retrieveFactoryBinder.addBinding().to(FhirRestRetrieveProviderFactory.class).in(Singleton.class);

    // Multibinder<org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory>
    // libraryContentProviderFactoryBinder =
    // Multibinder.newSetBinder(binder(),
    // org.opencds.cqf.cql.evaluator.builder.library.TypedLibraryContentProviderFactory.class);

    // libraryContentProviderFactoryBinder.addBinding().to(CqlFileLibraryContentProviderFactory.class);
    // libraryContentProviderFactoryBinder.addBinding().to(FhirFileLibraryContentProviderFactory.class);
    // libraryContentProviderFactoryBinder.addBinding().to(FhirRestLibraryContentProviderFactory.class);

    // Multibinder<org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory>
    // terminologyProviderFactoryBinder =
    // Multibinder.newSetBinder(binder(),
    // org.opencds.cqf.cql.evaluator.builder.terminology.TypedTerminologyProviderFactory.class);

    // terminologyProviderFactoryBinder.addBinding().to(FhirRestTerminologyProviderFactory.class);
    // terminologyProviderFactoryBinder.addBinding().to(FhirFileTerminologyProviderFactory.class);
    // }

    @Provides
    @IntoSet
    ModelResolverFactory fhirModelResolverFactory(FhirModelResolverFactory fhirModelResolverFactory) {
        return fhirModelResolverFactory;
    }

    @Provides
    RetrieveProviderConfig retrieveProviderConfig() {
        return RetrieveProviderConfig.defaultConfig();
    }

    @Provides
    @ElementsIntoSet
    Set<TypedLibraryContentProviderFactory> typedLibraryContentProviderFactories(CqlFileLibraryContentProviderFactory cqlFileLibraryContentProviderFactory, FhirFileLibraryContentProviderFactory fhirFileLibraryContentProviderFactory, FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory) {
        return new HashSet<>(Arrays.asList(cqlFileLibraryContentProviderFactory, fhirFileLibraryContentProviderFactory, fhirRestLibraryContentProviderFactory));
    }

    @Provides
    @ElementsIntoSet
    Set<TypedRetrieveProviderFactory> typedRetrieveProviderFactories(FhirRestRetrieveProviderFactory fhirRestRetrieveProviderFactory, FhirFileRetrieveProviderFactory fhirFileRetrieveProviderFactory) {
        return new HashSet<>(Arrays.asList(fhirRestRetrieveProviderFactory, fhirFileRetrieveProviderFactory));
    }

    @Provides
    @ElementsIntoSet
    Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories(FhirFileTerminologyProviderFactory fhirFileTerminologyProviderFactory, FhirRestTerminologyProviderFactory fhirRestTerminologyProviderFactory) {
        return new HashSet<>(Arrays.asList(fhirFileTerminologyProviderFactory, fhirRestTerminologyProviderFactory));
    }
}