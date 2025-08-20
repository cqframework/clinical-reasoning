package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdHelperTest {
    private static final String ID = "123";

    @Test
    void testIdType_whenResourceIdIsNull_willreturnsNull() {
        IIdType patientId = IdHelper.getIdType(FhirVersionEnum.R4, "Patient", null);
        assertNull(patientId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "/123", "Patient/123"})
    void testIdType_whenResourceIdIsNull_willreturnsNull(String id) {
        String resourceType = "Patient";
        IIdType patientId = IdHelper.getIdType(FhirVersionEnum.R4, resourceType, id);

        assertEquals(ID, patientId.getIdPart());
    }
}
