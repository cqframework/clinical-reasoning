package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.execution.ExpressionResult;

// LUKETODO:  javadoc
public record MeasureObservationResult(String expressionName, ExpressionResult expressionResult) {}
