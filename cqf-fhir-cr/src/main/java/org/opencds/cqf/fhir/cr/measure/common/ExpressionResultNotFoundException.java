package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Capture a post CQL evaluation error in which an expression result was not found when expected.
 */
public class ExpressionResultNotFoundException extends RuntimeException {
    public ExpressionResultNotFoundException(String expressionType, String expressionName) {
        super("Expression result for type: '%s' and expression name: '%s' not found"
                .formatted(expressionType, expressionName));
    }
}
