package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class HashSetForFhirResourcesTest {

    public static final String PATIENT_ID_1 = "patient-1";
    public static final String PATIENT_ID_2 = "patient-2";

    @Test
    void addFhirResourceWithSameIdIsNotAddedTwice() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = new Patient();
        patient1.setId(PATIENT_ID_1);
        var patient2 = new Patient();
        patient2.setId(PATIENT_ID_1);

        assertTrue(set.add(patient1));
        assertFalse(set.add(patient2));
        assertEquals(1, set.size());
    }

    @Test
    void addNonFhirObjectBehavesLikeHashSet() {
        var set = new HashSetForFhirResources<String>();
        assertTrue(set.add("foo"));
        assertFalse(set.add("foo"));
        assertEquals(1, set.size());
    }

    @Test
    void removeFhirResourceByIdRemovesCorrectResource() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        set.add(patient1);
        set.add(patient2);

        var removalCandidate = createPatientWithId(PATIENT_ID_2);
        set.remove(removalCandidate);

        assertEquals(1, set.size());
        assertTrue(set.contains(patient1));
    }

    @Test
    void retainAllKeepsOnlyMatchingFhirResourcesById() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        set.add(patient1);
        set.add(patient2);

        var retainPatient = createPatientWithId(PATIENT_ID_1);
        set.retainAll(List.of(retainPatient));

        assertTrue(set.contains(patient1));
        assertFalse(set.contains(patient2));
        assertEquals(1, set.size());
    }

    @Test
    void removeAllRemovesMatchingFhirResourcesById() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        set.add(patient1);
        set.add(patient2);

        var removePatient = createPatientWithId(PATIENT_ID_1);
        set.removeAll(List.of(removePatient));

        assertTrue(set.contains(patient2));
        assertFalse(set.contains(patient1));
        assertEquals(1, set.size());
    }

    @Test
    void addAllAddsOnlyNonDuplicateFhirResourcesById() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        set.add(patient1);

        var newPatient1 = createPatientWithId(PATIENT_ID_1);
        var newPatient3 = createPatientWithId("patient-3");
        set.addAll(List.of(newPatient1, newPatient3));

        assertTrue(set.contains(patient1));
        assertFalse(set.contains(patient2));
        assertEquals(2, set.size());
    }

    @Test
    void addDifferentFhirResourceTypesWithSameIdAreBothAdded() {
        var set = new HashSetForFhirResources<IBaseResource>();
        var patient = createPatientWithId("shared-id");
        var observation = createObservationWithId("shared-id");

        assertTrue(set.add(patient));
        assertTrue(set.add(observation));
        assertEquals(2, set.size());
    }

    @Test
    void addFhirResourceWithNullIdTwiceAddsOnlyOne() {
        var set = new HashSetForFhirResources<Patient>();
        var patient1 = new Patient();
        var patient2 = new Patient();

        assertTrue(set.add(patient1));
        assertFalse(set.add(patient2));
        assertEquals(1, set.size());
    }

    @Test
    void containsWithNullArgumentReturnsFalse() {
        var set = new HashSetForFhirResources<Patient>();
        assertFalse(set.contains(null));
    }

    private static Patient createPatientWithId(String id) {
        var patient = new Patient();
        patient.setId(id);
        return patient;
    }

    private static Observation createObservationWithId(String id) {
        var observation = new Observation();
        observation.setId(id);
        return observation;
    }
}
