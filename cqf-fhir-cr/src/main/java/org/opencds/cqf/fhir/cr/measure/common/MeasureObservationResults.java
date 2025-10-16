package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.List;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

public class MeasureObservationResults {
    private final List<MeasureObservationResult> measureObservationResults;

    static final MeasureObservationResults EMPTY = new MeasureObservationResults(List.of());

    MeasureObservationResults(List<MeasureObservationResult> measureObservationResults) {
        this.measureObservationResults = measureObservationResults;
    }

    EvaluationResult withNewEvaluationResult(EvaluationResult origEvaluationResult) {
        final EvaluationResult evaluationResult = new EvaluationResult();

        var copyOfExpressionResults = new HashMap<>(origEvaluationResult.expressionResults);

        measureObservationResults.forEach(measureObservationResult -> copyOfExpressionResults.put(
                measureObservationResult.getExpressionName(), buildExpressionResult(measureObservationResult)));

        evaluationResult.expressionResults.putAll(copyOfExpressionResults);

        return evaluationResult;
    }

    private ExpressionResult buildExpressionResult(MeasureObservationResult measureObservationResult) {
        return new ExpressionResult(
                measureObservationResult.getFunctionResults(), measureObservationResult.getEvaluatedResources());
    }
}
