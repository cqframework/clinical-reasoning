package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
        var id = Ids.newId(FhirContext.forDstu3Cached(), "Patient/123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
    }

    @Test
    void partsSupported() {
        var id = Ids.newId(FhirVersionEnum.DSTU3, "Patient", "123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);

        assertEquals("Patient", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }

    @Test
    void classSupported() {
        var id = Ids.newId(org.hl7.fhir.dstu3.model.Library.class, "123");
        assertTrue(id instanceof org.hl7.fhir.dstu3.model.IdType);
        assertEquals("Library", id.getResourceType());
        assertEquals("123", id.getIdPart());
    }

    @Test
    void ensureIdTypeWithSlash() {
        assertEquals("Patient/123", Ids.ensureIdType("Patient/123", "Patient"));
    }

    @Test
    void ensureIdTypeWithoutSlash() {
        assertEquals("Patient/123", Ids.ensureIdType("123", "Patient"));
    }

    @Test
    void newIdWithContext() {
        var id = Ids.newId(FhirContext.forR4Cached(), "Patient", "456");
        assertTrue(id instanceof org.hl7.fhir.r4.model.IdType);
        assertEquals("Patient", id.getResourceType());
        assertEquals("456", id.getIdPart());
    }

    @Test
    void newRandomId() {
        var id = Ids.newRandomId(FhirContext.forR4Cached(), "Patient");
        assertNotNull(id);
        assertEquals("Patient", id.getResourceType());
        assertNotNull(id.getIdPart());
    }

    @Test
    void newIdWithBaseTypeClass() {
        var id = Ids.newId(org.hl7.fhir.r4.model.StringType.class, "Library", "test-123");
        assertTrue(id instanceof org.hl7.fhir.r4.model.IdType);
        assertEquals("Library", id.getResourceType());
        assertEquals("test-123", id.getIdPart());
    }

    @Test
    void simpleFromResource() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("Patient/123");
        assertEquals("Patient/123", Ids.simple(patient));
    }

    @Test
    void simpleFromIdType() {
        var id = new org.hl7.fhir.r4.model.IdType("Patient/123");
        assertEquals("Patient/123", Ids.simple(id));
    }

    @Test
    void simplePartFromResource() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setId("Patient/123");
        assertEquals("123", Ids.simplePart(patient));
    }

    @Test
    void simplePartFromIdType() {
        var id = new org.hl7.fhir.r4.model.IdType("Patient/123");
        assertEquals("123", Ids.simplePart(id));
    }

    @Test
    void simpleThrowsForIdWithoutResourceType() {
        var id = new org.hl7.fhir.r4.model.IdType("123");
        assertThrows(IllegalArgumentException.class, () -> Ids.simple(id));
    }

    @Test
    void r5Version() {
        var id = Ids.newId(FhirVersionEnum.R5, "Patient/789");
        assertTrue(id instanceof org.hl7.fhir.r5.model.IdType);
    }
}
