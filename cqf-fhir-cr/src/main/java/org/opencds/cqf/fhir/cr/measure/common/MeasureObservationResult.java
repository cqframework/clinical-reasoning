package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MeasureObservationResult {
    private final String expressionName;
    private final Set<Object> evaluatedResources;
    private final Map<Object, Object> functionResults;

    static final MeasureObservationResult EMPTY = new MeasureObservationResult(null, Set.of(), Map.of());

    MeasureObservationResult(
            String expressionName, Set<Object> evaluatedResources, Map<Object, Object> functionResults) {
        this.expressionName = expressionName;
        this.evaluatedResources = evaluatedResources;
        this.functionResults = functionResults;
    }

    String getExpressionName() {
        return expressionName;
    }

    Map<Object, Object> getFunctionResults() {
        return new HashMap<>(functionResults);
    }

    Set<Object> getEvaluatedResources() {
        return new HashSetForFhirResources<>(evaluatedResources);
    }
}
