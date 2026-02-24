package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdHelperTest {
    private static final String ID = "123";

    @Test
    void testIdType_whenResourceIdIsNull_willreturnsNull() {
        var patientId = IdHelper.getIdType(FhirVersionEnum.R4, "Patient", (String) null);
        assertNull(patientId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "/123", "Patient/123"})
    void testIdType_whenResourceIdIsNull_willreturnsNull(String id) {
        var resourceType = "Patient";
        var patientId = IdHelper.getIdType(FhirVersionEnum.R4, resourceType, id);

        assertEquals(ID, patientId.getIdPart());
    }

    @Test
    void testIdType_whenIdIsDataType() {
        var id = new StringType(ID);
        var patientId = IdHelper.getIdType(FhirVersionEnum.R4, "Patient", id);
        assertEquals(ID, patientId.getIdPart());
    }
}
