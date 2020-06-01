package org.opencds.cqf.cql.evaluator;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.evaluator.resolver.ParameterDeserializer;
import org.opencds.cqf.cql.evaluator.resolver.implementation.DefaultParameterDeserializer;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.CqlEngine.Options;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

// TODO: Add debug options
public class CqlEvaluator {

    private LibraryLoader libraryLoader;

    private ParameterDeserializer parameterDeserializer;
    private VersionedIdentifier libraryIdentifier;

    private CqlEngine cqlEngine;

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName) {
        this(libraryLoader, libraryName, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier) {
        this(libraryLoader, libraryIdentifier, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, Map<String, DataProvider> dataProviders,
            TerminologyProvider terminologyProvider) {
        this(libraryLoader, libraryName, dataProviders, terminologyProvider, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier,
            Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider) {
        this(libraryLoader, libraryIdentifier, dataProviders, terminologyProvider, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, EnumSet<Options> engineOptions) {
        this(libraryLoader, libraryName, null, null, engineOptions, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier,
            EnumSet<Options> engineOptions) {
        this(libraryLoader, libraryIdentifier, null, null, engineOptions, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, Map<String, DataProvider> dataProviders,
            TerminologyProvider terminologyProvider, EnumSet<Options> engineOptions,
            ParameterDeserializer parameterResolver) {
        this(libraryLoader, new VersionedIdentifier().withId(libraryName), dataProviders, terminologyProvider,
                engineOptions, parameterResolver);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier,
            Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider,
            EnumSet<Options> engineOptions, ParameterDeserializer parameterDeserializer) {
        this.libraryLoader = Objects.requireNonNull(libraryLoader, "libraryLoader can not be null.");
        this.libraryIdentifier = Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null.");

        if (parameterDeserializer == null) {
            this.parameterDeserializer = new DefaultParameterDeserializer();
        }

        this.cqlEngine = new CqlEngine(this.libraryLoader, dataProviders, terminologyProvider, engineOptions);
    }

    public EvaluationResult evaluate() {
        return this.evaluate(null, null, null);
    }

    public EvaluationResult evaluate(Set<String> expressions) {
        return this.evaluate(expressions, null, null);
    }

    public EvaluationResult evaluate(Set<String> expressions, Pair<String, Object> contextParameter) {
        return this.evaluate(expressions, contextParameter, null);
    }

    public EvaluationResult evaluate(Set<String> expressions, Map<String, Object> parameters) {
        return this.evaluate(expressions, null, parameters);
    }

    public EvaluationResult evaluate(Pair<String, Object> contextParameter) {
        return this.evaluate(null, contextParameter, null);
    }

    public EvaluationResult evaluate(Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        return this.evaluate(null, contextParameter, parameters);
    }

    public EvaluationResult evaluate(Map<String, Object> parameters) {
        return this.evaluate(null, null, parameters);
    }

    public EvaluationResult evaluate(Set<String> expressions, Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        return this.cqlEngine.evaluate(this.libraryIdentifier, contextParameter, parameters);
    }

    public Pair<String, Object> unmarshalContextParameter(Pair<String, String> contextParameter) {
        return this.parameterDeserializer.deserializeContextParameter(contextParameter);
    }

    public Map<String, Object> unmarshalParameters(Map<String, String> parameters) {
       return this.parameterDeserializer.deserializeParameters(parameters);
    }
}