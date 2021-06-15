package org.opencds.cqf.cql.evaluator.dagger.builder;

import javax.inject.Singleton;

import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryContentProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class BuilderBindingModule {

    @Binds
    @Singleton
    abstract LibraryContentProviderFactory libraryLoaderFactory(org.opencds.cqf.cql.evaluator.builder.library.LibraryContentProviderFactory libraryLoaderFactory);


    @Binds
    @Singleton
    abstract DataProviderFactory dataProviderFactory(org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory dataProviderFactory);

    @Binds
    @Singleton
    abstract TerminologyProviderFactory terminologyProviderFactory(org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory terminologyProviderFactory);

    @Binds
    @Singleton
    abstract RetrieveProviderConfigurer retrieveProviderConfigurer(org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer retrieveProviderConfigurer);
}