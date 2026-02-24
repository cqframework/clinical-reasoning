package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;

/**
 * DO NOT CONVERT THIS TO A RECORD!
 * We must enforce instance equality, not value equality.  QuantityDef(2.0) and Quantity(2.0) are
 * considered NOT EQUAL for Set comparison.
 * FHIR-version-agnostic representation of a Quantity value.
 * Used for continuous variable measure scoring to avoid coupling to specific FHIR versions.
 * <p/>
 * Only stores the numeric value - unit, system, and code are not needed for scoring calculations.
 *
 * @see CodeDef
 * @see ConceptDef
 */
public class QuantityDef {

    public static QuantityDef fromBigDecimal(BigDecimal bigDecimal) {
        return new QuantityDef(bigDecimal.doubleValue());
    }

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
