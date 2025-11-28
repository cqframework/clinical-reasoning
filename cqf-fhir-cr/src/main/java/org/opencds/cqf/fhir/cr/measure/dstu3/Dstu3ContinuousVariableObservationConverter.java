package org.opencds.cqf.fhir.cr.measure.dstu3;

import jakarta.annotation.Nullable;
import org.hl7.fhir.dstu3.model.Quantity;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationConverter;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;

/**
 * DSTU3 version of {@link ContinuousVariableObservationConverter}, with the singleton pattern
 * enforced by an enum.
 *
 * This implementation only handles DSTU3-specific Quantity conversions.
 * All common logic (Number, String, null handling) is in the interface default methods.
 */
@SuppressWarnings("squid:S6548")
public enum Dstu3ContinuousVariableObservationConverter implements ContinuousVariableObservationConverter<Quantity> {
    INSTANCE;

    // Added by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Extract QuantityDef from DSTU3 Quantity.
     * Returns null if result is not a DSTU3 Quantity (allowing default method to handle other types).
     */
    @Nullable
    @Override
    public QuantityDef extractQuantityDef(Object result) {
        if (result instanceof Quantity existing) {
            return new QuantityDef(existing.hasValue() ? existing.getValue().doubleValue() : null);
        }
        return null;
    }

    // Enhanced by Claude Sonnet 4.5 on 2025-11-27
    /**
     * Convert QuantityDef to DSTU3 Quantity for MeasureReport population.
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
        return quantity;
    }
}
