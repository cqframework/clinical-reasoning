package org.opencds.cqf.cql.evaluator.builder;

import org.opencds.cqf.cql.engine.model.ModelResolver;

public interface ModelResolverFactory {
    public String getModelUri();
    public ModelResolver create(String version);
}