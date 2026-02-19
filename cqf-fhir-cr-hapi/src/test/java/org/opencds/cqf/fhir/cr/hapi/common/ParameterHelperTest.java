package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringOrReferenceValue;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getValue;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

class ParameterHelperTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void testGetValue_ReturnsNullForNullParameter() {
        assertNull(getValue(FhirVersionEnum.R4, null));
    }

    @Test
    void testGetStringOrReferenceValue() {
        var patientId = "test";
        var stringParam = (IBaseBackboneElement) newStringPart(fhirContextR4, "stringParam", patientId);
        var stringResult = getStringOrReferenceValue(FhirVersionEnum.R4, stringParam);
        assertEquals(patientId, stringResult);
        var referenceParam =
                (IBaseBackboneElement) newPart(fhirContextR4, Reference.class, "referenceParam", patientId);
        var referenceResult = getStringOrReferenceValue(FhirVersionEnum.R4, referenceParam);
        assertEquals(patientId, referenceResult);
    }

    @Test
    void testGetStringValue() {
        var stringValue = "test";
        var stringParam = (IBaseBackboneElement) newStringPart(fhirContextR4, "stringParam", stringValue);
        assertEquals(stringValue, getStringValue(FhirVersionEnum.R4, stringParam));
        var canonicalType = new CanonicalType(stringValue);
        assertEquals(stringValue, getStringValue(canonicalType));
    }
}
