package org.opencds.cqf.fhir.cr.measure.r4;

import jakarta.annotation.Nullable;
import org.hl7.fhir.r4.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;

/**
 * R4 version of {@link ContinuousVariableObservationConverter}, with the singleton pattern
 * enforced by an enum.
 *
 * This implementation only handles R4-specific Quantity conversions.
 * All common logic (Number, String, null handling) is in the interface default methods.
 */
@SuppressWarnings("squid:S6548")
public enum R4ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    // Added by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Extract QuantityDef from R4 Quantity.
     * Returns null if result is not an R4 Quantity (allowing default method to handle other types).
     */
    @Nullable
    @Override
    public QuantityDef extractQuantityDef(Object result) {
        if (result instanceof Quantity existing) {
            return new QuantityDef(
                    existing.hasValue() ? existing.getValue().doubleValue() : null,
                    existing.getUnit(),
                    existing.getSystem(),
                    existing.getCode());
        }
        return null;
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Convert QuantityDef to R4 Quantity for MeasureReport population.
     */
    @Override
    public Quantity convertToFhirQuantity(QuantityDef quantityDef) {
        if (quantityDef == null) {
            return null;
        }

        Quantity quantity = new Quantity();
        Double value = quantityDef.value();
        if (value != null) {
            quantity.setValue(value);
        }
        if (quantityDef.unit() != null) {
            quantity.setUnit(quantityDef.unit());
        }
        if (quantityDef.system() != null) {
            quantity.setSystem(quantityDef.system());
        }
        if (quantityDef.code() != null) {
            quantity.setCode(quantityDef.code());
        }
        return quantity;
    }
}
