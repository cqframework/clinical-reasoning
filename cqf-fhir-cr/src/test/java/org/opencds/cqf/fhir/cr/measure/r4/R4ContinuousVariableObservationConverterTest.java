package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.*;

import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.def.report.QuantityReportDef;

// Updated by Claude Sonnet 4.5 on 2025-12-02
// Trimmed to only test convertToFhirQuantity() - conversion FROM CQL is now in ContinuousVariableObservationHandler
class R4ContinuousVariableObservationConverterTest {

    @Test
    void testConvertQuantityDefToR4Quantity() {
        var converter = R4ContinuousVariableObservationConverter.INSTANCE;

        QuantityReportDef qd = new QuantityReportDef(75.0);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertEquals(75.0, result.getValue().doubleValue());
        // Note: unit, system, code are not preserved - only value matters for scoring
        assertNull(result.getUnit());
        assertNull(result.getSystem());
        assertNull(result.getCode());
    }

    @Test
    void testConvertQuantityDefWithValueOnly() {
        var converter = R4ContinuousVariableObservationConverter.INSTANCE;

        QuantityReportDef qd = new QuantityReportDef(42.0);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertEquals(42.0, result.getValue().doubleValue());
        assertNull(result.getUnit());
        assertNull(result.getSystem());
        assertNull(result.getCode());
    }

    @Test
    void testConvertNullQuantityDefReturnsNull() {
        var converter = R4ContinuousVariableObservationConverter.INSTANCE;

        assertNull(converter.convertToFhirQuantity(null));
    }

    @Test
    void testConvertQuantityDefWithNullValue() {
        var converter = R4ContinuousVariableObservationConverter.INSTANCE;

        QuantityReportDef qd = new QuantityReportDef(null);

        Quantity result = converter.convertToFhirQuantity(qd);

        assertNotNull(result);
        assertFalse(result.hasValue());
        assertNull(result.getUnit());
    }
}
