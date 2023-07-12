package org.opencds.cqf.cql.evaluator;

import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.CqlEngine.Options;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

// TODO: Add debug options
public class CqlEvaluator {

  private Environment environment;
  private CqlEngine cqlEngine;

  public CqlEvaluator(Environment environment) {
    this(environment, null);
  }

  public Environment getEnvironment() {
    return this.environment;
  }

  public CqlEvaluator(Environment environment, Set<Options> engineOptions) {
    this.environment = requireNonNull(environment, "libraryLoader can not be null.");
    this.cqlEngine = new CqlEngine(this.environment, EnumSet.copyOf(engineOptions));
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier) {
    return this.evaluate(libraryIdentifier, null, null, null);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions) {
    return this.evaluate(libraryIdentifier, expressions, null, null);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions,
      Pair<String, Object> contextParameter) {
    return this.evaluate(libraryIdentifier, expressions, contextParameter, null);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions,
      Map<String, Object> parameters) {
    return this.evaluate(libraryIdentifier, expressions, null, parameters);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier,
      Pair<String, Object> contextParameter) {
    return this.evaluate(libraryIdentifier, null, contextParameter, null);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier,
      Pair<String, Object> contextParameter, Map<String, Object> parameters) {
    return this.evaluate(libraryIdentifier, null, contextParameter, parameters);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier,
      Map<String, Object> parameters) {
    return this.evaluate(libraryIdentifier, null, null, parameters);
  }

  public EvaluationResult evaluate(VersionedIdentifier libraryIdentifier, Set<String> expressions,
      Pair<String, Object> contextParameter, Map<String, Object> parameters) {
    return this.cqlEngine.evaluate(libraryIdentifier, expressions, contextParameter, parameters,
        null);
  }
}
