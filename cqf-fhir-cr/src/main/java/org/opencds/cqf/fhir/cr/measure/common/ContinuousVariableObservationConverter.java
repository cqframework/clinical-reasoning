package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.ICompositeType;

/**
 * Convert continuous variable scoring function-returned resources to QuantityDef
 * and back to FHIR-specific Quantities when needed.
 *
 * This interface contains all common conversion logic in default methods.
 * Implementations only need to handle FHIR-version-specific Quantity conversions.
 */
public interface ContinuousVariableObservationConverter<T extends ICompositeType> {

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Convert CQL evaluation result to version-agnostic QuantityDef.
     *
     * This default implementation handles all common types (Number, String).
     * Delegates to extractQuantityDef() for FHIR-specific Quantity conversion.
     *
     * @param result the Object from CQL evaluation (Number, String, or FHIR Quantity)
     * @return QuantityDef representation
     * @throws IllegalArgumentException if result cannot be converted
     */
    default QuantityDef wrapResultAsQuantity(Object result) {
        if (result == null) {
            return null;
        }

        // Delegate FHIR Quantity extraction to implementor
        QuantityDef fromQuantity = extractQuantityDef(result);
        if (fromQuantity != null) {
            return fromQuantity;
        }

        // Handle Number (common logic)
        if (result instanceof Number number) {
            return new QuantityDef(number.doubleValue());
        }

        // Handle String (common logic with validation)
        if (result instanceof String s) {
            try {
                return new QuantityDef(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("String is not a valid number: " + s, e);
            }
        }

        // Unknown type - throw exception
        throw new IllegalArgumentException("Cannot convert object of type " + result.getClass() + " to QuantityDef");
    }

    // Added by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Extract QuantityDef from FHIR-specific Quantity.
     *
     * Implementors should check if result is their FHIR version's Quantity type
     * and extract the value. Return null if not a Quantity.
     *
     * @param result the potential FHIR Quantity object
     * @return QuantityDef if result is a FHIR Quantity, null otherwise
     */
    @Nullable
    QuantityDef extractQuantityDef(Object result);

    // Added by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Convert QuantityDef back to FHIR-specific Quantity for MeasureReport population.
     *
     * Implementors create their FHIR version's Quantity and populate the value field.
     *
     * @param quantityDef the version-agnostic quantity
     * @return FHIR-specific Quantity (R4 or DSTU3)
     */
    T convertToFhirQuantity(QuantityDef quantityDef);
}
