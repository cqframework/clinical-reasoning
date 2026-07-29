package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Base class for measure evaluation errors caused by invalid client input — the request itself is
 * malformed or refers to resources that cannot be resolved. Subclasses are translated to {@code
 * ca.uhn.fhir.rest.server.exceptions.InvalidRequestException} (HTTP 400) at the HAPI provider
 * boundary so that a single {@code catch} can cover the whole family.
 *
 * <p>Service-layer code in {@code cqf-fhir-cr} should throw a subclass of this type rather than a
 * HAPI exception directly, keeping the service layer independent of HAPI's HTTP bindings.
 */
public class InvalidMeasureRequestException extends RuntimeException {
    public InvalidMeasureRequestException(String message) {
        super(message);
    }

    public InvalidMeasureRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
