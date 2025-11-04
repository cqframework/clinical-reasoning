package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Precision;

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
        assertFalse(set.contains(patient2));
    }

    @Test
    void removeCqlDateRemovesCorrectCqlDate() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.add(date1);
        set.add(date2);

        var removalCandidate = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.remove(removalCandidate);

        assertEquals(1, set.size());
        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
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
    void retainAllKeepsOnlyMatchingCqlDate() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.add(date1);
        set.add(date2);

        var retainDate = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        set.retainAll(List.of(retainDate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
        assertEquals(1, set.size());
    }

    @Test
    void retainAllKeepsOnlyMatchingCqlDateWithMatchingPrecision() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.DAY);
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.DAY);
        set.add(date1);
        set.add(date2);

        var retainDate = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.DAY);
        set.retainAll(List.of(retainDate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
        assertEquals(1, set.size());
    }

    @Test
    void retainAllKeepsOnlyMatchingCqlDateWithPrecisionMismatch() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.DAY);
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.HOUR);
        set.add(date1);
        set.add(date2);

        // equals logic considers the actual precision, not the intended precision
        var retainDate = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.MINUTE);
        set.retainAll(List.of(retainDate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
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
    void removeAllRemovesMatchingCqlDate() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.add(date1);
        set.add(date2);

        var removalCandidate = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.removeAll(List.of(removalCandidate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
        assertEquals(1, set.size());
    }

    @Test
    void removeAllRemovesMatchingCqlDateWithPrecision() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.DAY);
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.DAY);
        set.add(date1);
        set.add(date2);

        var removalCandidate = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.DAY);
        set.removeAll(List.of(removalCandidate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
        assertEquals(1, set.size());
    }

    @Test
    void removeAllRemovesMatchingCqlDateMismatchPrecision() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.DAY);
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.DAY);
        set.add(date1);
        set.add(date2);

        // Actual precision matters for comparison, not intended precision
        var removalCandidate = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.MINUTE);
        set.removeAll(List.of(removalCandidate));

        assertTrue(set.contains(date1));
        assertFalse(set.contains(date2));
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
    void addAllAddsNoDuplicateCqlDates() {
        var set = new HashSetForFhirResources<Date>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        set.add(date1);
        set.add(date2);

        var newDate1 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        var newDate2 = new Date(LocalDate.of(2026, Month.JANUARY, 1));
        set.addAll(List.of(newDate1, newDate2));

        assertTrue(set.contains(date1));
        assertTrue(set.contains(date2));
        assertTrue(set.contains(newDate2));
        assertEquals(3, set.size());
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
        assertFalse(new HashSetForFhirResources<Patient>().contains(null));
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
