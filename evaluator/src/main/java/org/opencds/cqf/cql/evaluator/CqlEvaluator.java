package org.opencds.cqf.cql.evaluator;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.evaluator.resolver.ParameterResolver;
import org.opencds.cqf.cql.evaluator.resolver.implementation.DefaultParameterResolver;
import org.opencds.cqf.cql.execution.CqlEngine;
import org.opencds.cqf.cql.execution.CqlEngine.Options;
import org.opencds.cqf.cql.execution.EvaluationResult;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

// TODO: Add debug options
public class CqlEvaluator {

    private LibraryLoader libraryLoader; 

    private ParameterResolver parameterResolver;
    private VersionedIdentifier libraryIdentifier;

    private CqlEngine cqlEngine;

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName) {
        this(libraryLoader, libraryName, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier) {
        this(libraryLoader, libraryIdentifier, null, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider) {
        this(libraryLoader, libraryName, dataProviders, terminologyProvider, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider) {
        this(libraryLoader, libraryIdentifier, dataProviders, terminologyProvider, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, EnumSet<Options> engineOptions) {
        this(libraryLoader, libraryName, null, null, engineOptions, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, EnumSet<Options> engineOptions) {
        this(libraryLoader, libraryIdentifier, null, null, engineOptions, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader, String libraryName, Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider, EnumSet<Options> engineOptions, ParameterResolver parameterResolver) {
        this(libraryLoader, new VersionedIdentifier().withId(libraryName), dataProviders, terminologyProvider, engineOptions, parameterResolver);
    }   

    public CqlEvaluator(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier, Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider, EnumSet<Options> engineOptions, ParameterResolver parameterResolver) {

        if (libraryLoader == null) {
            throw new IllegalArgumentException("libraryLoader can not be null.");
        }

        this.libraryLoader = libraryLoader;

        if (libraryIdentifier == null) {
            throw new IllegalArgumentException("libraryIdentifier can not be null.");
        }

        this.libraryIdentifier= libraryIdentifier;

        if (parameterResolver == null) {
            parameterResolver = new DefaultParameterResolver(this.libraryLoader);
        }

        this.cqlEngine = new CqlEngine(libraryLoader, dataProviders, terminologyProvider, engineOptions);
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

    public Pair<String, Object> resolveContextParameter(Pair<String, String> unresolvedContextParameter) {
        return this.parameterResolver.resolveContextParameters(this.libraryIdentifier, unresolvedContextParameter);
    }

    public Map<String, Object> resolveParameters(Map<String, String> unresolvedParameters) {
       return this.parameterResolver.resolveParameters(this.libraryIdentifier, unresolvedParameters);
    }
}