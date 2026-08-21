package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when a Measure resource fails structural validation at MeasureDef build time — e.g. a
 * stratifier with no {@code criteria.expression} and no components, or other shape errors that make
 * the Measure un-evaluable.
 */
public class InvalidMeasureDefinitionException extends InvalidMeasureRequestException {
    public InvalidMeasureDefinitionException(String message) {
        super(message);
    }
}
