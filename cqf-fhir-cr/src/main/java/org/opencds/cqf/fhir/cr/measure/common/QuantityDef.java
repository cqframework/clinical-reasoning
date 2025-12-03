package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

// Updated by Claude Sonnet 4.5 on 2025-12-02
/**
 * FHIR-version-agnostic representation of a Quantity value.
 * Used for continuous variable measure scoring to avoid coupling to specific FHIR versions.
 *
 * Only stores the numeric value - unit, system, and code are not needed for scoring calculations.
 *
 * @see CodeDef
 * @see ConceptDef
 */
public class QuantityDef {

    @Nullable
    private final Double value;

    public QuantityDef(@Nullable Double value) {
        this.value = value;
    }

    @Nullable
    public Double value() {
        return value;
    }

    @Override
    public String toString() {
        return "QuantityDef{value=" + value + '}';
    }
}
