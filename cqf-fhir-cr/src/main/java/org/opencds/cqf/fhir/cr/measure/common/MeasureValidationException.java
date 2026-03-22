package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when a Measure resource is structurally invalid: bad scoring type,
 * missing required populations, duplicate IDs, invalid extensions, or
 * parameters that violate the measure specification.
 *
 * <p>Transport adapters should map this to HTTP 400 (Bad Request).
 */
public class MeasureValidationException extends MeasureException {

    public MeasureValidationException(String message) {
        super(message);
    }

    public MeasureValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
