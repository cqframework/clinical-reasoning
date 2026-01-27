package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.cql.engine.runtime.Date;

class HashMapForFhirResourcesAndCqlTypesTest {

    public static final String ENCOUNTER_ID_1 = "encounter-1";
    public static final String ENCOUNTER_ID_2 = "encounter-2";
    public static final String PATIENT_ID_1 = "patient-1";
    public static final String PATIENT_ID_2 = "patient-2";

    // ========== containsKey tests ==========

    @Test
    void containsKeyWithFhirResourceByIdReturnsTrue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        assertTrue(map.containsKey(lookupEncounter));
    }

    @Test
    void containsKeyWithFhirResourceDifferentIdReturnsFalse() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_2);
        assertFalse(map.containsKey(lookupEncounter));
    }

    @Test
    void containsKeyWithCqlDateReturnsTrue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Date, String>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        map.put(date1, "value1");

        var lookupDate = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        assertTrue(map.containsKey(lookupDate));
    }

    @Test
    void containsKeyWithNonFhirNonCqlTypeBehavesLikeHashMap() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, String>();
        map.put("key1", "value1");

        assertTrue(map.containsKey("key1"));
        assertFalse(map.containsKey("key2"));
    }

    // ========== containsValue tests ==========

    @Test
    void containsValueWithFhirResourceByIdReturnsTrue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        map.put("key1", patient1);

        var lookupPatient = createPatientWithId(PATIENT_ID_1);
        assertTrue(map.containsValue(lookupPatient));
    }

    @Test
    void containsValueWithFhirResourceDifferentIdReturnsFalse() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, Patient>();
        var patient1 = createPatientWithId(PATIENT_ID_1);
        map.put("key1", patient1);

        var lookupPatient = createPatientWithId(PATIENT_ID_2);
        assertFalse(map.containsValue(lookupPatient));
    }

    // ========== get tests ==========

    @Test
    void getWithFhirResourceKeyByIdReturnsValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "expected-value");

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        assertEquals("expected-value", map.get(lookupEncounter));
    }

    @Test
    void getWithFhirResourceKeyDifferentIdReturnsNull() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_2);
        assertNull(map.get(lookupEncounter));
    }

    @Test
    void getWithCqlDateKeyReturnsValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Date, String>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        map.put(date1, "expected-value");

        var lookupDate = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        assertEquals("expected-value", map.get(lookupDate));
    }

    // ========== put tests ==========

    @Test
    void putWithSameFhirResourceIdReplacesValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "original-value");

        var encounter2 = createEncounterWithId(ENCOUNTER_ID_1); // Same ID, different object
        String oldValue = map.put(encounter2, "new-value");

        assertEquals("original-value", oldValue);
        assertEquals(1, map.size());
        assertEquals("new-value", map.get(encounter1));
    }

    @Test
    void putWithDifferentFhirResourceIdAddsEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var encounter2 = createEncounterWithId(ENCOUNTER_ID_2);

        map.put(encounter1, "value1");
        map.put(encounter2, "value2");

        assertEquals(2, map.size());
        assertEquals("value1", map.get(encounter1));
        assertEquals("value2", map.get(encounter2));
    }

    @Test
    void putWithSameCqlDateReplacesValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Date, String>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        map.put(date1, "original-value");

        var date2 = new Date(LocalDate.of(2024, Month.JANUARY, 1)); // Same date, different object
        String oldValue = map.put(date2, "new-value");

        assertEquals("original-value", oldValue);
        assertEquals(1, map.size());
        assertEquals("new-value", map.get(date1));
    }

    // ========== remove(key) tests ==========

    @Test
    void removeWithFhirResourceKeyByIdRemovesEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var encounter2 = createEncounterWithId(ENCOUNTER_ID_2);
        map.put(encounter1, "value1");
        map.put(encounter2, "value2");

        var removalCandidate = createEncounterWithId(ENCOUNTER_ID_1); // Different object, same ID
        String removedValue = map.remove(removalCandidate);

        assertEquals("value1", removedValue);
        assertEquals(1, map.size());
        assertFalse(map.containsKey(encounter1));
        assertTrue(map.containsKey(encounter2));
    }

    @Test
    void removeWithCqlDateKeyRemovesEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Date, String>();
        var date1 = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        var date2 = new Date(LocalDate.of(2025, Month.JANUARY, 1));
        map.put(date1, "value1");
        map.put(date2, "value2");

        var removalCandidate = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        String removedValue = map.remove(removalCandidate);

        assertEquals("value1", removedValue);
        assertEquals(1, map.size());
        assertFalse(map.containsKey(date1));
        assertTrue(map.containsKey(date2));
    }

    @Test
    void removeWithNonExistentKeyReturnsNull() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var nonExistentEncounter = createEncounterWithId(ENCOUNTER_ID_2);
        assertNull(map.remove(nonExistentEncounter));
        assertEquals(1, map.size());
    }

    // ========== remove(key, value) tests ==========

    @Test
    void removeKeyValueWithMatchingFhirResourcesRemovesEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var patient1 = createPatientWithId(PATIENT_ID_1);
        map.put(encounter1, patient1);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        var lookupPatient = createPatientWithId(PATIENT_ID_1);

        assertTrue(map.remove(lookupEncounter, lookupPatient));
        assertEquals(0, map.size());
    }

    @Test
    void removeKeyValueWithMatchingKeyButDifferentValueReturnsFalse() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var patient1 = createPatientWithId(PATIENT_ID_1);
        map.put(encounter1, patient1);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        var differentPatient = createPatientWithId(PATIENT_ID_2);

        assertFalse(map.remove(lookupEncounter, differentPatient));
        assertEquals(1, map.size());
    }

    // ========== getOrDefault tests ==========

    @Test
    void getOrDefaultWithExistingFhirResourceKeyReturnsValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "expected-value");

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        assertEquals("expected-value", map.getOrDefault(lookupEncounter, "default"));
    }

    @Test
    void getOrDefaultWithNonExistentKeyReturnsDefault() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var nonExistentEncounter = createEncounterWithId(ENCOUNTER_ID_2);
        assertEquals("default", map.getOrDefault(nonExistentEncounter, "default"));
    }

    @Test
    void getOrDefaultWithExistingKeyAndNullValueReturnsNull() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, null);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        assertNull(map.getOrDefault(lookupEncounter, "default"));
    }

    // ========== putIfAbsent tests ==========

    @Test
    void putIfAbsentWithExistingFhirResourceKeyDoesNotReplace() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "original-value");

        var encounter2 = createEncounterWithId(ENCOUNTER_ID_1);
        String existingValue = map.putIfAbsent(encounter2, "new-value");

        assertEquals("original-value", existingValue);
        assertEquals(1, map.size());
        assertEquals("original-value", map.get(encounter1));
    }

    @Test
    void putIfAbsentWithNonExistentKeyAddsEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);

        String result = map.putIfAbsent(encounter1, "value1");

        assertNull(result);
        assertEquals(1, map.size());
        assertEquals("value1", map.get(encounter1));
    }

    // ========== replace(key, value) tests ==========

    @Test
    void replaceWithExistingFhirResourceKeyReplacesValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "original-value");

        var encounter2 = createEncounterWithId(ENCOUNTER_ID_1);
        String oldValue = map.replace(encounter2, "new-value");

        assertEquals("original-value", oldValue);
        assertEquals("new-value", map.get(encounter1));
    }

    @Test
    void replaceWithNonExistentKeyReturnsNull() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter1, "value1");

        var nonExistentEncounter = createEncounterWithId(ENCOUNTER_ID_2);
        assertNull(map.replace(nonExistentEncounter, "new-value"));
        assertEquals(1, map.size());
    }

    // ========== replace(key, oldValue, newValue) tests ==========

    @Test
    void replaceWithMatchingOldValueReplacesValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        map.put(encounter1, patient1);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        var lookupOldPatient = createPatientWithId(PATIENT_ID_1);

        assertTrue(map.replace(lookupEncounter, lookupOldPatient, patient2));
        assertEquals(PATIENT_ID_2, map.get(encounter1).getIdElement().getIdPart());
    }

    @Test
    void replaceWithNonMatchingOldValueReturnsFalse() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);
        map.put(encounter1, patient1);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        var wrongOldPatient = createPatientWithId("wrong-id");

        assertFalse(map.replace(lookupEncounter, wrongOldPatient, patient2));
        assertEquals(PATIENT_ID_1, map.get(encounter1).getIdElement().getIdPart());
    }

    // ========== Mixed resource type tests ==========

    @Test
    void differentFhirResourceTypesWithSameIdAreBothStored() {
        var map = new HashMapForFhirResourcesAndCqlTypes<IBaseResource, String>();
        var patient = createPatientWithId("shared-id");
        var observation = createObservationWithId("shared-id");

        map.put(patient, "patient-value");
        map.put(observation, "observation-value");

        assertEquals(2, map.size());
        assertEquals("patient-value", map.get(patient));
        assertEquals("observation-value", map.get(observation));
    }

    // ========== Copy constructor tests ==========

    @Test
    void copyConstructorPreservesCustomEquality() {
        var originalMap = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        originalMap.put(encounter1, "value1");

        var copiedMap = new HashMapForFhirResourcesAndCqlTypes<>(originalMap);

        var lookupEncounter = createEncounterWithId(ENCOUNTER_ID_1);
        assertEquals("value1", copiedMap.get(lookupEncounter));
    }

    // ========== toString tests ==========

    @Test
    void toStringWithEmptyMapReturnsEmptyBraces() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, String>();

        assertEquals("{}", map.toString());
    }

    @Test
    void toStringWithSinglePrimitiveEntry() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, String>();
        map.put("key1", "value1");

        assertEquals("{key1=value1}", map.toString());
    }

    @Test
    void toStringWithMultiplePrimitiveEntries() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, Integer>();
        map.put("age", 42);
        map.put("count", 100);

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("age=42"));
        assertTrue(result.contains("count=100"));
        assertTrue(result.contains(", "));
    }

    @Test
    void toStringWithFhirResourceKeyWithId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, String>();
        var encounter = createEncounterWithId(ENCOUNTER_ID_1);
        map.put(encounter, "value1");

        // getValueAsString() returns just the ID part, not "ResourceType/id"
        assertEquals("{encounter-1=value1}", map.toString());
    }

    @Test
    void toStringWithFhirResourceValueWithId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, Patient>();
        var patient = createPatientWithId(PATIENT_ID_1);
        map.put("patient", patient);

        // getValueAsString() returns just the ID part, not "ResourceType/id"
        assertEquals("{patient=patient-1}", map.toString());
    }

    @Test
    void toStringWithFhirResourceKeyAndValueWithIds() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter = createEncounterWithId(ENCOUNTER_ID_1);
        var patient = createPatientWithId(PATIENT_ID_1);
        map.put(encounter, patient);

        // Both IDs without resource type prefix
        assertEquals("{encounter-1=patient-1}", map.toString());
    }

    @Test
    void toStringWithFhirResourceWithoutId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Patient, String>();
        var patient = new Patient(); // No ID set
        map.put(patient, "value1");

        // getValueAsString() on null ID returns "null"
        assertEquals("{null=value1}", map.toString());
    }

    @Test
    void toStringWithFhirResourceValueWithoutId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, Encounter>();
        var encounter = new Encounter(); // No ID set
        map.put("encounter", encounter);

        // getValueAsString() on null ID returns "null"
        assertEquals("{encounter=null}", map.toString());
    }

    @Test
    void toStringWithMultipleFhirResources() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, Patient>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var encounter2 = createEncounterWithId(ENCOUNTER_ID_2);
        var patient1 = createPatientWithId(PATIENT_ID_1);
        var patient2 = createPatientWithId(PATIENT_ID_2);

        map.put(encounter1, patient1);
        map.put(encounter2, patient2);

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("encounter-1=patient-1"));
        assertTrue(result.contains("encounter-2=patient-2"));
        assertTrue(result.contains(", "));
    }

    @Test
    void toStringWithMixedFhirAndPrimitiveTypes() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Object, Object>();
        var encounter = createEncounterWithId(ENCOUNTER_ID_1);
        map.put("primitive-key", "primitive-value");
        map.put(encounter, 42);

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("primitive-key=primitive-value"));
        assertTrue(result.contains("encounter-1=42"));
        assertTrue(result.contains(", "));
    }

    @Test
    void toStringWithNullValue() {
        var map = new HashMapForFhirResourcesAndCqlTypes<String, String>();
        map.put("key-with-null", null);

        assertEquals("{key-with-null=null}", map.toString());
    }

    @Test
    void toStringWithCqlDateKey() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Date, String>();
        var date = new Date(LocalDate.of(2024, Month.JANUARY, 1));
        map.put(date, "value1");

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("=value1"));
        // CQL Date toString format may vary
    }

    @Test
    void toStringWithVersionedResourceId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Patient, String>();
        var patient = new Patient();
        patient.setId("Patient/patient-1/_history/2");
        map.put(patient, "value1");

        // getValueAsString() returns the full versioned ID
        String result = map.toString();
        assertTrue(result.contains("patient-1"));
        assertTrue(result.contains("value1"));
    }

    @Test
    void toStringWithComplexNestedStructure() {
        var map = new HashMapForFhirResourcesAndCqlTypes<Encounter, QuantityDef>();
        var encounter1 = createEncounterWithId(ENCOUNTER_ID_1);
        var encounter2 = createEncounterWithId(ENCOUNTER_ID_2);
        var quantity1 = new QuantityDef(100.0);
        var quantity2 = new QuantityDef(200.0);

        map.put(encounter1, quantity1);
        map.put(encounter2, quantity2);

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("encounter-1=QuantityDef{value=100.0}"));
        assertTrue(result.contains("encounter-2=QuantityDef{value=200.0}"));
        assertTrue(result.contains(", "));
    }

    @Test
    void toStringWithDifferentResourceTypesWithSameId() {
        var map = new HashMapForFhirResourcesAndCqlTypes<IBaseResource, String>();
        var patient = createPatientWithId("shared-id");
        var observation = createObservationWithId("shared-id");

        map.put(patient, "patient-value");
        map.put(observation, "observation-value");

        String result = map.toString();
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        // Both should be in the string - just the ID part without resource type
        assertTrue(result.contains("shared-id=patient-value"));
        assertTrue(result.contains("shared-id=observation-value"));
        assertTrue(result.contains(", "));
    }

    // ========== Helper methods ==========

    private static Encounter createEncounterWithId(String id) {
        var encounter = new Encounter();
        encounter.setId(id);
        return encounter;
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
