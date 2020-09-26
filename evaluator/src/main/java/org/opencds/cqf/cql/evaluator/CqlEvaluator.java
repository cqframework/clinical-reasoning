package org.opencds.cqf.cql.evaluator;

import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.CqlEngine.Options;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;

// TODO: Add debug options
public class CqlEvaluator {

    private LibraryLoader libraryLoader;
    private Map<String, DataProvider> dataProviders;
    private TerminologyProvider terminologyProvider;
    private CqlEngine cqlEngine;

    public CqlEvaluator(LibraryLoader libraryLoader) {
        this(libraryLoader, null, null, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader,  Map<String, DataProvider> dataProviders,
            TerminologyProvider terminologyProvider) {
        this(libraryLoader, dataProviders, terminologyProvider, null);
    }

    public CqlEvaluator(LibraryLoader libraryLoader,
            EnumSet<Options> engineOptions) {
        this(libraryLoader, null, null, engineOptions);
    }

    public LibraryLoader getLibraryLoader() {
        return this.libraryLoader;
    }

    public Map<String, DataProvider> getDataProviders() {
        return this.dataProviders;
    }

    public TerminologyProvider getTerminologyProvider() {
        return this.terminologyProvider;
    }

    public CqlEvaluator(LibraryLoader libraryLoader,
            Map<String, DataProvider> dataProviders, TerminologyProvider terminologyProvider,
            EnumSet<Options> engineOptions) {
        this.libraryLoader = requireNonNull(libraryLoader, "libraryLoader can not be null.");
        this.cqlEngine = new CqlEngine(this.libraryLoader, dataProviders, terminologyProvider, engineOptions);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier) {
        return this.evaluate(libraryIdentifier, null, null, null);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions) {
        return this.evaluate(libraryIdentifier, expressions, null, null);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions, Pair<String, Object> contextParameter) {
        return this.evaluate(libraryIdentifier, expressions, contextParameter, null);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions, Map<String, Object> parameters) {
        return this.evaluate(libraryIdentifier, expressions, null, parameters);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter) {
        return this.evaluate(libraryIdentifier, null, contextParameter, null);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        return this.evaluate(libraryIdentifier, null, contextParameter, parameters);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Map<String, Object> parameters) {
        return this.evaluate(libraryIdentifier, null, null, parameters);
    }

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier,
    Set<String> expressions, Pair<String, Object> contextParameter, Map<String, Object> parameters) {
        return this.cqlEngine.evaluate(libraryIdentifier, expressions, contextParameter, parameters, null);
    }
}