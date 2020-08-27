package org.opencds.cqf.cql.evaluator.library.di;

import com.google.inject.AbstractModule;

import org.opencds.cqf.cql.evaluator.library.api.LibraryEvaluator;

public class LibraryModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(LibraryEvaluator.class).to(org.opencds.cqf.cql.evaluator.library.LibraryEvaluator.class);
    }
    
}