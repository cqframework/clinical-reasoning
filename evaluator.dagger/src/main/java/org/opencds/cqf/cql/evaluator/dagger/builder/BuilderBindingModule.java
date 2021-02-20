package org.opencds.cqf.cql.evaluator.dagger.builder;

import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class BuilderBindingModule {

    @Binds
    abstract LibraryLoaderFactory libraryLoaderFactory(org.opencds.cqf.cql.evaluator.builder.library.LibraryLoaderFactory libraryLoaderFactory);


    @Binds
    abstract DataProviderFactory dataProviderFactory(org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory dataProviderFactory);

    @Binds
    abstract TerminologyProviderFactory terminologyProviderFactory(org.opencds.cqf.cql.evaluator.builder.terminology.TerminologyProviderFactory terminologyProviderFactory);

    @Binds
    abstract RetrieveProviderConfigurer retrieveProviderConfigurer(org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer retrieveProviderConfigurer);
}