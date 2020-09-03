package org.opencds.cqf.cql.evaluator.library;

import com.google.inject.AbstractModule;

public class LibraryModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(LibraryEvaluator.class).to(InjectableLibraryEvaluator.class);
    }
    
}