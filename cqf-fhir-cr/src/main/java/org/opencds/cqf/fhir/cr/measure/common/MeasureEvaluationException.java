package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when CQL evaluation or function execution fails at runtime,
 * or when an internal invariant is violated during measure processing.
 *
 * <p>Transport adapters should map this to HTTP 500 (Internal Server Error).
 */
public class MeasureEvaluationException extends MeasureException {

    public MeasureEvaluationException(String message) {
        super(message);
    }

    public MeasureEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
