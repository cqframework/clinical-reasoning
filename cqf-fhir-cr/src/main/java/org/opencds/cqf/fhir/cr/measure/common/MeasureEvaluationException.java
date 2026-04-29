package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Base class for runtime exceptions raised during Measure evaluation. Subclasses categorize the
 * failure (e.g. invalid Measure definition, missing expression result).
 */
public class MeasureEvaluationException extends RuntimeException {
    public MeasureEvaluationException(String message) {
        super(message);
    }

    public MeasureEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
