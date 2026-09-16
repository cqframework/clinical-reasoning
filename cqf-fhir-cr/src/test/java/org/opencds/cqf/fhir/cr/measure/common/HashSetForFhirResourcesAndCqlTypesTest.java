package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Date;
import org.opencds.cqf.cql.engine.runtime.Decimal;
import org.opencds.cqf.cql.engine.runtime.Precision;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

class HashSetForFhirResourcesAndCqlTypesTest {

    public static final String PATIENT_ID_1 = "patient-1";
    public static final String PATIENT_ID_2 = "patient-2";
    public static final String ENCOUNTER_ID = "encounter-1";

    @Test
    void addFhirResourceWithSameIdIsNotAddedTwice() {
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<String>();
        assertTrue(set.add("foo"));
        assertFalse(set.add("foo"));
        assertEquals(1, set.size());
    }

    @Test
    void removeFhirResourceByIdRemovesCorrectResource() {
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();
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
        var set = new HashSetForFhirResourcesAndCqlTypes<IBaseResource>();
        var patient = createPatientWithId("shared-id");
        var observation = createObservationWithId("shared-id");

        assertTrue(set.add(patient));
        assertTrue(set.add(observation));
        assertEquals(2, set.size());
    }

    @Test
    void addFhirResourceWithNullIdTwiceAddsOnlyOne() {
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
        var patient1 = new Patient();
        var patient2 = new Patient();

        assertTrue(set.add(patient1));
        assertFalse(set.add(patient2));
        assertEquals(1, set.size());
    }

    @Test
    void containsWithNullArgumentReturnsFalse() {
        assertFalse(new HashSetForFhirResourcesAndCqlTypes<Patient>().contains(null));
    }

    @Test
    void retainAllWithUncertainCqlTypeComparison() {
        // Test scenario: CQL Date comparison with mismatched precision levels can result in null equality
        // This tests the defensive null handling in areEqualCqlTypes

        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();

        // Add dates with YEAR precision - these have limited precision
        var date1Year = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.YEAR);
        var date2Year = new Date(LocalDate.of(2024, Month.JUNE, 15), Precision.YEAR);

        set.add(date1Year);
        set.add(date2Year);

        // Create a date with DAY precision to compare against YEAR precision dates
        // In CQL, comparing dates with different precision levels can have special semantics
        var compareDate = new Date(LocalDate.of(2024, Month.MARCH, 10), Precision.DAY);

        // The retainAll operation should handle any null returns from equal() gracefully
        // Even if the comparison is uncertain, the operation should complete without NPE
        set.retainAll(List.of(compareDate));

        // Verify the operation completed successfully (no NPE thrown)
        // The exact result depends on CQL equality semantics, but we care that it doesn't crash
        assertTrue(true, "Operation should complete without throwing NPE");
    }

    @Test
    void removeWithDifferentPrecisionDoesNotThrowNPE() {
        // Test the remove operation with dates that might have uncertain equality
        var set = new HashSetForFhirResourcesAndCqlTypes<Date>();

        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1), Precision.YEAR);
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1), Precision.MONTH);

        set.add(date1);
        set.add(date2);

        // Try to remove with different precision - should not throw NPE
        var removalDate = new Date(LocalDate.of(2024, Month.DECEMBER, 31), Precision.DAY);

        // Should complete without NPE regardless of equality result
        set.remove(removalDate);

        // Just verify no exception was thrown - if we got here, the test passed
        assertTrue(true, "Remove operation should complete without NPE");
    }

    // ==================== One relation across every operation ====================

    /**
     * The set holds elements under a single identity, so nothing it accepted can be absent from it.
     * <p/>
     * These are the value shapes the measure pipeline puts in these sets. Before the set was keyed,
     * {@code add} and {@code contains} reached for different notions of equality and could disagree
     * about the same element; the CQL {@code Decimal} case below is one that actually did.
     */
    @Test
    void addThenContainsAgreeForEveryValueShape() {
        var values = List.of(
                createPatientWithId(PATIENT_ID_1),
                encounterInstance(ENCOUNTER_ID),
                new Date(LocalDate.of(2024, Month.JANUARY, 1)),
                new Decimal(new BigDecimal("1.0")),
                "a plain string");

        for (Object value : values) {
            var set = new HashSetForFhirResourcesAndCqlTypes<>();
            assertTrue(set.add(value), () -> "add returned false for " + value);
            assertTrue(set.contains(value), () -> "contains disagreed with add for " + value);
            assertFalse(set.add(value), () -> "add accepted a duplicate of " + value);
            assertEquals(1, set.size());
        }
    }

    /**
     * CQL {@code =} says {@code 1.0 = 1.00}; {@code Decimal.equals} is scale-sensitive and says the
     * opposite. Whichever the set uses, it has to use it for both operations.
     */
    @Test
    void addAndContainsAgreeOnCqlDecimalScale() {
        var set = new HashSetForFhirResourcesAndCqlTypes<Decimal>();
        var oneDecimalPlace = new Decimal(new BigDecimal("1.0"));
        var twoDecimalPlaces = new Decimal(new BigDecimal("1.00"));

        assertTrue(set.add(oneDecimalPlace));
        assertTrue(set.contains(twoDecimalPlaces));
        assertFalse(set.add(twoDecimalPlaces));
        assertEquals(1, set.size());
    }

    // ==================== Engine-native (ClassInstance) resources ====================

    /**
     * The same resource retrieved twice is one element even when the two copies differ in content -
     * a differing {@code meta.versionId} here. Structural comparison calls those two resources
     * distinct, and CQL {@code =} calls the comparison uncertain, so before id-keying a set of
     * evaluated resources could hold the same resource more than once.
     */
    @Test
    void engineNativeResourcesWithSameIdAreOneElement() {
        var set = new HashSetForFhirResourcesAndCqlTypes<ClassInstance>();

        assertTrue(set.add(encounterInstance(ENCOUNTER_ID)));
        assertFalse(set.add(encounterInstanceWithVersion(ENCOUNTER_ID, "7")));
        assertEquals(1, set.size());
    }

    @Test
    void engineNativeResourcesWithDifferentIdsAreDistinct() {
        var set = new HashSetForFhirResourcesAndCqlTypes<ClassInstance>();

        assertTrue(set.add(encounterInstance(ENCOUNTER_ID)));
        assertTrue(set.add(encounterInstance("encounter-2")));
        assertEquals(2, set.size());
    }

    /**
     * A resource reaches the pipeline either as a HAPI object or as an engine-native value, and it
     * is the same resource in both forms.
     */
    @Test
    void engineNativeAndHapiFormsOfOneResourceAreOneElement() {
        var set = new HashSetForFhirResourcesAndCqlTypes<>();
        var hapiEncounter = new Encounter();
        hapiEncounter.setId(ENCOUNTER_ID);

        assertTrue(set.add(hapiEncounter));
        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
        assertFalse(set.add(encounterInstance(ENCOUNTER_ID)));
        assertEquals(1, set.size());
    }

    /**
     * The population/stratifier intersection at {@code MeasureMultiSubjectEvaluator} retains against
     * a plain {@code List}, whose own {@code contains} compares by Java object identity. The
     * comparison has to run in this set's relation regardless of what it is handed.
     */
    @Test
    void retainAllAgainstPlainListOfEngineNativeResources() {
        var set = new HashSetForFhirResourcesAndCqlTypes<ClassInstance>();
        set.add(encounterInstance(ENCOUNTER_ID));
        set.add(encounterInstance("encounter-2"));

        set.retainAll(List.of(encounterInstance(ENCOUNTER_ID)));

        assertEquals(1, set.size());
        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
        assertFalse(set.contains(encounterInstance("encounter-2")));
    }

    @Test
    void removeAllAgainstPlainListOfEngineNativeResources() {
        var set = new HashSetForFhirResourcesAndCqlTypes<ClassInstance>();
        set.add(encounterInstance(ENCOUNTER_ID));
        set.add(encounterInstance("encounter-2"));

        set.removeAll(List.of(encounterInstance("encounter-2")));

        assertEquals(1, set.size());
        assertTrue(set.contains(encounterInstance(ENCOUNTER_ID)));
    }

    @Test
    void iterationOrderFollowsInsertion() {
        var set = new HashSetForFhirResourcesAndCqlTypes<Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        set.add(patient1);
        set.add(patient2);

        assertEquals(List.of(patient1, patient2), List.copyOf(set));
    }

    static ClassInstance encounterInstance(String id) {
        var encounter = new Encounter();
        encounter.setId(id);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        return toEngineNative(encounter);
    }

    private static ClassInstance encounterInstanceWithVersion(String id, String versionId) {
        var encounter = new Encounter();
        encounter.setId(id);
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.getMeta().setVersionId(versionId);
        return toEngineNative(encounter);
    }

    private static ClassInstance toEngineNative(IBaseResource resource) {
        return (ClassInstance)
                FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4).toCqlValue(resource, false);
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
