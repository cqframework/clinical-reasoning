package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Set;

/**
 * A glorified Pair to capture both evaluation results and evaluated resources.
 */
// LUKETODO:  think about whether or not we need this
public record ObservationEvaluationResult(Object result, Set<Object> evaluatedResources) {}
