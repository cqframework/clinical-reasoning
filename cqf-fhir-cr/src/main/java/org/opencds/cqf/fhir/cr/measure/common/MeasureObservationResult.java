package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Capture a single set of results from continuous variable observations for a single population
 */
// LUKETODO:  think about this:
// LUKETODO:  I did this because I wanted a structure that I could mark as EMPTY and use an an input for building a new
// EvaluationResult
public record MeasureObservationResult(
        String expressionName, Set<Object> evaluatedResources, Map<Object, Object> functionResults) {

    static final MeasureObservationResult EMPTY = new MeasureObservationResult(null, Set.of(), Map.of());

    @Override
    public Map<Object, Object> functionResults() {
        return new HashMap<>(functionResults);
    }

    @Override
    public Set<Object> evaluatedResources() {
        return new HashSetForFhirResources<>(evaluatedResources);
    }
}
