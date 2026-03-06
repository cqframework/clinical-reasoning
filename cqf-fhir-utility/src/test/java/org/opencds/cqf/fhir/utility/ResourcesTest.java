package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class ResourcesTest {

    @Test
    void testClone() {
        var lib = new Library();
        lib.setUsage("example usage");

        var c = Resources.clone(lib);

        assertNotSame(c, lib);
        assertTrue(c.equalsDeep(lib));
    }

    @Test
    void testStringify() {
        var lib = new Library();
        var s = Resources.stringify(lib);
        assertTrue(s.contains("Library"));
    }

    @Test
    void castOrThrowNull() {
        var result = Resources.castOrThrow(null, Patient.class, "error");
        assertTrue(result.isEmpty());
    }

    @Test
    void castOrThrowValid() {
        var patient = new Patient();
        var result = Resources.castOrThrow(patient, Patient.class, "error");
        assertTrue(result.isPresent());
        assertEquals(patient, result.get());
    }

    @Test
    void castOrThrowInvalid() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> Resources.castOrThrow(library, Patient.class, "wrong type"));
    }

    @Test
    void newResourceWithId() {
        var patient = Resources.newResource(Patient.class, "123");
        assertNotNull(patient);
        assertEquals("123", patient.getIdElement().getIdPart());
    }

    @Test
    void newResourceWithIdContainingSlashThrows() {
        assertThrows(IllegalArgumentException.class, () -> Resources.newResource(Patient.class, "Patient/123"));
    }

    @Test
    void newResourceWithoutId() {
        var patient = Resources.newResource(Patient.class);
        assertNotNull(patient);
    }

    @Test
    void newBackboneElement() {
        var entry = Resources.newBackboneElement(org.hl7.fhir.r4.model.Bundle.BundleEntryComponent.class);
        assertNotNull(entry);
    }

    @Test
    void newBaseForVersion() {
        var patient = Resources.newBaseForVersion("Patient", FhirVersionEnum.R4);
        assertNotNull(patient);
    }

    @Test
    void newBase() {
        var patient = Resources.newBase(Patient.class);
        assertNotNull(patient);
    }

    @Test
    void getClassForTypeAndVersion() {
        var clazz = Resources.getClassForTypeAndVersion("Patient", FhirVersionEnum.R4);
        assertEquals(Patient.class, clazz);
    }

    @Test
    void getClassForTypeAndVersionInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Resources.getClassForTypeAndVersion("NonExistentType", FhirVersionEnum.R4));
    }
}
