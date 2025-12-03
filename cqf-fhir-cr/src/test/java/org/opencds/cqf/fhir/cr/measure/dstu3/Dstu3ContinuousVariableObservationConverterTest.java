package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.dstu3.model.Quantity;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.QuantityDef;

// Updated by Claude Sonnet 4.5 on 2025-12-02
// Trimmed to only test convertToFhirQuantity() - conversion FROM CQL is now in ContinuousVariableObservationHandler
class Dstu3ContinuousVariableObservationConverterTest {

    @Test
    void testConvertQuantityDefToDstu3Quantity() {
        var converter = Dstu3ContinuousVariableObservationConverter.INSTANCE;

        QuantityDef qd = new QuantityDef(75.0);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertEquals(75.0, result.getValue().doubleValue());
        // Note: unit, system, code are not preserved - only value matters for scoring
        assertNull(result.getUnit());
        assertNull(result.getSystem());
        assertNull(result.getCode());
    }

    @Test
    void testConvertQuantityDefWithValueOnly() {
        var converter = Dstu3ContinuousVariableObservationConverter.INSTANCE;

        QuantityDef qd = new QuantityDef(42.0);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertEquals(42.0, result.getValue().doubleValue());
        assertNull(result.getUnit());
        assertNull(result.getSystem());
        assertNull(result.getCode());
    }

    @Test
    void testConvertNullQuantityDefReturnsNull() {
        var converter = Dstu3ContinuousVariableObservationConverter.INSTANCE;

        assertNull(converter.convertToFhirQuantity(null));
    }

    @Test
    void testConvertQuantityDefWithNullValue() {
        var converter = Dstu3ContinuousVariableObservationConverter.INSTANCE;

        QuantityDef qd = new QuantityDef(null);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertNotNull(result);
        assertFalse(result.hasValue());
        assertNull(result.getUnit());
    }
}
