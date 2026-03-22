package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Base exception for all measure evaluation failures.
 *
 * <p>Subclasses distinguish the category of failure so that transport adapters
 * can map each to the appropriate HTTP status code without string-matching
 * on messages.
 */
public class MeasureException extends RuntimeException {

    public MeasureException(String message) {
        super(message);
    }

    public MeasureException(String message, Throwable cause) {
        super(message, cause);
    }
}
