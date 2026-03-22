package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when score calculation fails — for example, a numeric conversion
 * error, a missing observation population, or an unsupported aggregate method.
 *
 * <p>Transport adapters should map this to HTTP 500 (Internal Server Error).
 */
public class MeasureScoringException extends MeasureException {

    public MeasureScoringException(String message) {
        super(message);
    }

    public MeasureScoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
