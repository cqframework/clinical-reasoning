package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Thrown when the request cannot be resolved to a unique Measure to evaluate — e.g. the measure
 * URL matches no resources, matches multiple resources, or no measure reference was supplied at
 * all.
 */
public class MeasureLookupException extends InvalidMeasureRequestException {
    public MeasureLookupException(String message) {
        super(message);
    }
}
