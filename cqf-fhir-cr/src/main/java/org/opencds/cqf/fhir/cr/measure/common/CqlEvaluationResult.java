package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

public class CqlEvaluationResult {
    private final EvaluationResult result;
    private List<CqlExpressionValue> expressionResults;

    public CqlEvaluationResult() {
        this(new EvaluationResult());
    }

    public CqlEvaluationResult(EvaluationResult result) {
        this.result = result;
        expressionResults = new ArrayList<>();
        if (!this.result.getExpressionResults().isEmpty()) {
            expressionResults.addAll(this.result.getExpressionResults().entrySet().stream()
                    .map(entry -> CqlExpressionValue.of(entry.getKey(), entry.getValue()))
                    .toList());
        }
    }

    public EvaluationResult getResult() {
        return result;
    }

    public void setExpressionResults(List<CqlExpressionValue> expressionResults) {
        this.expressionResults = expressionResults;
    }

    public List<CqlExpressionValue> getExpressionResults() {
        return expressionResults;
    }

    public void addExpressionResult(CqlExpressionValue result) {
        expressionResults.add(result);
    }

    public CqlExpressionValue get(String expression) {
        return StringUtils.isBlank(expression)
                ? null
                : expressionResults.stream()
                        .filter(r -> expression.equals(r.expressionName()))
                        .findFirst()
                        .orElse(null);
    }
}
