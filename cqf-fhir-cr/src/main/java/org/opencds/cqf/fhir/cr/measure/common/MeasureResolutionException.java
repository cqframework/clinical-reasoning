package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when a Measure, Library, or subject resource cannot be found
 * or resolved from the repository.
 *
 * <p>Transport adapters should map this to HTTP 404 (Not Found).
 */
public class MeasureResolutionException extends MeasureException {

    public MeasureResolutionException(String message) {
        super(message);
    }

    public MeasureResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
