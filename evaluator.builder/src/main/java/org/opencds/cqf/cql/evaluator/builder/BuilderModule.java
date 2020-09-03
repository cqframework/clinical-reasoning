package org.opencds.cqf.cql.evaluator.builder;

import com.google.inject.AbstractModule;

import org.opencds.cqf.cql.evaluator.ParameterParser;
import org.opencds.cqf.cql.evaluator.builder.common.CommonModule;

public class BuilderModule extends AbstractModule {

    @Override
    protected void configure() {
        this.install(new CommonModule());

        // TODO - HACK: The serialization framework isn't ready to go just yet.
        this.bind(ParameterParser.class).to(org.opencds.cqf.cql.evaluator.common.ParameterParser.class);
    }
    
}