package org.opencds.cqf.cql.evaluator.guice;

import com.google.inject.AbstractModule;

import org.opencds.cqf.cql.evaluator.ParameterParser;

public class EvaluatorModule extends AbstractModule {

    @Override
    protected void configure() {
        this.bind(ParameterParser.class).to(org.opencds.cqf.cql.evaluator.common.ParameterParser.class);
    }
    
}
