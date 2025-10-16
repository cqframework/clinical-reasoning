package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Set;

/**
 * A glorified Pair to capture both evaluation results and evaluated resources.
 */
public record ObservationEvaluationResult(Object result, Set<Object> evaluatedResources) {}
