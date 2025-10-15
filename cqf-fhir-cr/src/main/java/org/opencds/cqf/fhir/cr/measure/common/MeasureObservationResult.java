package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.ExpressionResult;

/**
 * Capture the results of continuous variable evaluation to be added to {@link org.opencds.cqf.cql.engine.execution.EvaluationResult}s
 */
public record MeasureObservationResult(String expressionName, ExpressionResult expressionResult) {}
