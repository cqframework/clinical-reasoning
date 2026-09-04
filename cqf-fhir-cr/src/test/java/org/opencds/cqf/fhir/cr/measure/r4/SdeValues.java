package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

/**
 * Reads the converted supplemental-data values out of a {@link MeasureDef}. A MeasureReport only
 * carries a reference to an SDE resource (it is an evaluated resource, so it is not contained), so
 * the resource itself is only reachable through the def.
 * <p>
 * Accumulation deliberately leaves resource-valued SDEs in their engine-native {@link ClassInstance}
 * form, converting only what a report builder renders, so this converts the same way the builders do.
 */
final class SdeValues {

    private SdeValues() {}

    static <T> T onlySupplementalDataResource(MeasureDef measureDef, Class<T> type) {
        var sdes = measureDef.sdes();
        assertEquals(1, sdes.size());
        var values = sdes.get(0).getAccumulatedValues().keySet().stream()
                .map(wrapper -> toFhir(wrapper.getValue()))
                .toList();
        assertEquals(1, values.size());
        var value = values.get(0);
        assertNotNull(value);
        assertEquals(type, value.getClass());
        return type.cast(value);
    }

    private static Object toFhir(Object value) {
        return value instanceof ClassInstance classInstance
                ? ClassInstanceHelper.convertToFhirR4(classInstance)
                : value;
    }
}
