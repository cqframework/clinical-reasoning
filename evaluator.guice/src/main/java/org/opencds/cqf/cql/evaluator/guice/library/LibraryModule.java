package org.opencds.cqf.cql.evaluator.guice.library;

import com.google.inject.AbstractModule;

import org.opencds.cqf.cql.evaluator.library.*;

public class LibraryModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(LibraryEvaluator.class).to(LibraryEvaluator.class);
    }

}