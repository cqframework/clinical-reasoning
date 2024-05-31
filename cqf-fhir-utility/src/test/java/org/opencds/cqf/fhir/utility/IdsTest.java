package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;

class IdsTest {

    @Test
    void allVersionsSupported() {
        assertDoesNotThrow(() -> {
            for (FhirVersionEnum fhirVersionEnum : FhirVersionEnum.values()) {
                Ids.newId(fhirVersionEnum, "Patient/123");
            }
        });
    }

    @Test
    void contextSupported() {
        IIdType id = Ids.newId(FhirContext.forDstu3Cached(), "Patient/123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
    }

    @Test
    void partsSupported() {
        IIdType id = Ids.newId(FhirVersionEnum.DSTU3, "Patient", "123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);

        assertEquals("Patient", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }

    @Test
    void classSupported() {
        IIdType id = Ids.newId(org.hl7.fhir.dstu3.model.Library.class, "123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
        assertEquals("Library", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }
}
