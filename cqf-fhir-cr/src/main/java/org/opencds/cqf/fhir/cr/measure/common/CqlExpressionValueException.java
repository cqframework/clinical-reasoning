package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when a value extracted from a CQL {@code ExpressionResult} cannot be normalized
 * by {@link CqlExpressionValue} into the shape the caller expects (for example, when a
 * Boolean criterion's resolved subject-context lookup returns no result).
 */
public class CqlExpressionValueException extends RuntimeException {
    public CqlExpressionValueException(String message) {
        super(message);
    }
}
