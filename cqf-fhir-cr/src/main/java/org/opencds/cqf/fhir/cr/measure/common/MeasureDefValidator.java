package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Strategy interface for validating a {@link MeasureDef} before CQL evaluation begins.
 * Implementations check specific prerequisites (e.g. library resolution, ValueSet availability)
 * and return a {@link ValidationResult} containing any issues found.
 */
public interface MeasureDefValidator {
    ValidationResult validate(MeasureDefValidationContext context);
}
