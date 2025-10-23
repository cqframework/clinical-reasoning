package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.List;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

/**
 * Capture results for multiple continuous variable populations.
 */
public record MeasureObservationResults(List<MeasureObservationResult> results) {

    static final MeasureObservationResults EMPTY = new MeasureObservationResults(List.of());

    EvaluationResult withNewEvaluationResult(EvaluationResult origEvaluationResult) {
        final EvaluationResult evaluationResult = new EvaluationResult();

        var copyOfExpressionResults = new HashMap<>(origEvaluationResult.expressionResults);

        results.forEach(measureObservationResult -> copyOfExpressionResults.put(
                measureObservationResult.expressionName(), buildExpressionResult(measureObservationResult)));

        evaluationResult.expressionResults.putAll(copyOfExpressionResults);

        return evaluationResult;
    }

    private ExpressionResult buildExpressionResult(MeasureObservationResult measureObservationResult) {
        return new ExpressionResult(
                measureObservationResult.functionResults(), measureObservationResult.evaluatedResources());
    }
}
