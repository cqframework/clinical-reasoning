package org.opencds.cqf.cql.evaluator.api;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

public interface CqlEvaluator {

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier);
 
    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions, Pair<String, Object> contextParameter);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions, Map<String, Object> parameters);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Pair<String, Object> contextParameter, Map<String, Object> parameters);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Map<String, Object> parameters);

    public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier,
    Set<String> expressions, Pair<String, Object> contextParameter, Map<String, Object> parameters) ; 
}