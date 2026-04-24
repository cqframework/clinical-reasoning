package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when a non-subject value stratifier component references a CQL expression
 * that cannot be resolved in the library.
 */
public class StratifierExpressionNotFoundException extends RuntimeException {
    public StratifierExpressionNotFoundException(String expression, String measureUrl, Throwable cause) {
        super(
                "Non-subject value stratifier component expression '%s' could not be resolved for measure: %s"
                        .formatted(expression, measureUrl),
                cause);
    }
}
