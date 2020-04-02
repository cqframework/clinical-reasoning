package org.opencds.cqf.cql.evaluator.builder.context;

import java.util.EnumSet;
import java.util.Objects;

import org.opencds.cqf.cql.evaluator.builder.Options;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;
import org.opencds.cqf.cql.evaluator.resolver.implementation.DefaultParameterResolver;
import org.opencds.cqf.cql.execution.CqlEngine;

public class BuilderContext {

    private BuilderLibraryContext libraryContext = new BuilderLibraryContext();
    private BuilderDataContext dataContext = new BuilderDataContext();
    private BuilderTerminologyContext terminologyContext = new BuilderTerminologyContext();

    private ParameterResolver parameterResolver = new DefaultParameterResolver();

    private EnumSet<CqlEngine.Options> engineOptions = EnumSet.of(org.opencds.cqf.cql.execution.CqlEngine.Options.EnableExpressionCaching); 
    private EnumSet<Options> options = EnumSet.noneOf(Options.class);

    public BuilderDataContext getDataContext() {
        return this.dataContext;
    }

    public BuilderLibraryContext getLibraryContext() {
        return this.libraryContext;
    }

    public BuilderTerminologyContext getTerminologyContext() {
        return this.terminologyContext;
    }

    public EnumSet<CqlEngine.Options> getEngineOptions() {
        return this.engineOptions;
    }

    public void setEngineOptions(EnumSet<CqlEngine.Options> engineOptions) {
        Objects.requireNonNull(engineOptions, "engineOptions can not be null.");
    }

    public EnumSet<Options> getOptions() {
        return this.options;
    }

    public void setOptions(EnumSet<Options> options) {
        Objects.requireNonNull(options, "options can not be null.");
        this.options = options;
    }

    public ParameterResolver getParameterResolver() {
        return this.parameterResolver;
    }

    public void setParameterResolver(ParameterResolver parameterResolver) {
        Objects.requireNonNull(parameterResolver, "parameterResolver can not be null.");
        this.parameterResolver = parameterResolver;
    }
}