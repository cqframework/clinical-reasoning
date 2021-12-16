package org.opencds.cqf.cql.evaluator.dagger.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Singleton;

import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
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
import dagger.multibindings.IntoSet;

@Module(includes = { BuilderBindingModule.class, AdapterModule.class })
public class BuilderModule {
    @Provides
    @IntoSet
    @Singleton
    ModelResolverFactory fhirModelResolverFactorySet(FhirModelResolverFactory fhirModelResolverFactory) {
        return fhirModelResolverFactory;
    }

    @Provides
    @Singleton
    ModelResolverFactory fhirModelResolverFactory(FhirModelResolverFactory fhirModelResolverFactory) {
        return fhirModelResolverFactory;
    }   
      

    @Provides
    @Singleton
    RetrieveProviderConfig retrieveProviderConfig() {
        return RetrieveProviderConfig.defaultConfig();
    }

    @Provides
    @ElementsIntoSet
    @Singleton
    Set<TypedLibraryContentProviderFactory> typedLibraryContentProviderFactories(CqlFileLibraryContentProviderFactory cqlFileLibraryContentProviderFactory, FhirFileLibraryContentProviderFactory fhirFileLibraryContentProviderFactory, FhirRestLibraryContentProviderFactory fhirRestLibraryContentProviderFactory) {
        return new HashSet<>(Arrays.asList(cqlFileLibraryContentProviderFactory, fhirFileLibraryContentProviderFactory, fhirRestLibraryContentProviderFactory));
    }

    @Provides
    @ElementsIntoSet
    @Singleton
    Set<TypedRetrieveProviderFactory> typedRetrieveProviderFactories(FhirRestRetrieveProviderFactory fhirRestRetrieveProviderFactory, FhirFileRetrieveProviderFactory fhirFileRetrieveProviderFactory) {
        return new HashSet<>(Arrays.asList(fhirRestRetrieveProviderFactory, fhirFileRetrieveProviderFactory));
    }

    @Provides
    @ElementsIntoSet
    @Singleton
    Set<TypedTerminologyProviderFactory> typedTerminologyProviderFactories(FhirFileTerminologyProviderFactory fhirFileTerminologyProviderFactory, FhirRestTerminologyProviderFactory fhirRestTerminologyProviderFactory) {
        return new HashSet<>(Arrays.asList(fhirFileTerminologyProviderFactory, fhirRestTerminologyProviderFactory));
    }

    @Provides
    Supplier<CqlEvaluatorBuilder> cqlEvaluatorBuilderSupplier() {
        return () -> new CqlEvaluatorBuilder();
    }
}