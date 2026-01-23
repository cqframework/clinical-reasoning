package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

class PopulationDefTest {

    @Test
    void setHandlingStrings() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null, stringBasis, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null, stringBasis, null);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        popDef1.addResource("subj1", "string1");
        popDef2.addResource("subj1", "string1");

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains("string1"));
    }

    @Test
    void setHandlingIntegers() {
        CodeDef integerBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "integer");
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null, integerBasis, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null, integerBasis, null);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        popDef1.addResource("subj1", 123);
        popDef2.addResource("subj1", 123);

        popDef1.retainAllResources("subj1", popDef2);
        assertEquals(1, popDef1.getAllSubjectResources().size());
        assertTrue(popDef1.getAllSubjectResources().contains(123));
    }

    @Test
    void setHandlingEncounters() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        final PopulationDef popDef1 = new PopulationDef("one", null, null, null, encounterBasis, null);
        final PopulationDef popDef2 = new PopulationDef("two", null, null, null, encounterBasis, null);

        assertFalse(popDef1.isBooleanBasis());
        assertFalse(popDef2.isBooleanBasis());

        final Encounter enc1a = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));
        final Encounter enc1b = (Encounter) new Encounter().setId(new IdType(ResourceType.Encounter.name(), "enc1"));

        popDef1.addResource("subj1", enc1a);
        popDef2.addResource("subj1", enc1b);

        popDef1.retainAllResources("subj1", popDef2);

        assertEquals(1, popDef1.getAllSubjectResources().size());

        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1a));
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1b));
    }

    private Set<Object> getResourcesDistinctAcrossAllSubjects(PopulationDef popDef) {
        return new HashSetForFhirResourcesAndCqlTypes<>(popDef.getSubjectResources().values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Test isBooleanBasis() returns true for boolean basis.
     */
    @Test
    void testIsBooleanBasis_WithBooleanBasis() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);

        assertTrue(popDef.isBooleanBasis(), "Expected isBooleanBasis() to return true for boolean basis");
        assertEquals(booleanBasis, popDef.getPopulationBasis());
    }

    /**
     * Test isBooleanBasis() returns false for non-boolean basis types.
     */
    @Test
    void testIsBooleanBasis_WithNonBooleanBasis() {
        // Test various non-boolean basis types
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef encounterPop = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);
        assertFalse(encounterPop.isBooleanBasis(), "Expected isBooleanBasis() to return false for Encounter basis");

        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationDef stringPop =
                new PopulationDef("pop-2", null, MeasurePopulationType.DENOMINATOR, "Denominator", stringBasis, null);
        assertFalse(stringPop.isBooleanBasis(), "Expected isBooleanBasis() to return false for String basis");

        CodeDef dateBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "date");
        PopulationDef datePop =
                new PopulationDef("pop-3", null, MeasurePopulationType.NUMERATOR, "Numerator", dateBasis, null);
        assertFalse(datePop.isBooleanBasis(), "Expected isBooleanBasis() to return false for date basis");
    }

    /**
     * Test getCount() with boolean basis - counts unique subjects.
     */
    @Test
    void testGetCount_BooleanBasis_CountsUniqueSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);

        // Add 3 unique subjects
        popDef.addResource("Patient/1", true);
        popDef.addResource("Patient/2", true);
        popDef.addResource("Patient/3", true);

        assertTrue(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "Boolean basis should count unique subjects");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() with non-boolean basis - counts all resources including duplicates across subjects.
     */
    @Test
    void testGetCount_EncounterBasis_CountsAllResources() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);

        // Subject 1 has 2 encounters
        popDef.addResource("Patient/1", new Encounter().setId("Encounter/1"));
        popDef.addResource("Patient/1", new Encounter().setId("Encounter/2"));
        // Subject 2 has 3 encounters
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/3"));
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/4"));
        popDef.addResource("Patient/2", new Encounter().setId("Encounter/5"));

        assertFalse(popDef.isBooleanBasis());
        assertEquals(5, popDef.getCount(), "Encounter basis should count all resources");
        assertEquals(2, popDef.getSubjects().size(), "Should have 2 unique subjects");
    }

    /**
     * Test getCount() with String basis - counts all string resources.
     */
    @Test
    void testGetCount_StringBasis_CountsAllResources() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationDef popDef =
                new PopulationDef("pop-1", null, MeasurePopulationType.NUMERATOR, "Numerator", stringBasis, null);

        // Add string values for different subjects
        // Even if the same string value appears for different subjects, count all
        popDef.addResource("Patient/1", "value1");
        popDef.addResource("Patient/2", "value2");
        popDef.addResource("Patient/3", "value1"); // Duplicate value but different subject

        assertFalse(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "String basis should count all resources including duplicates");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() with date basis - counts all date resources.
     */
    @Test
    void testGetCount_DateBasis_CountsAllResources() {
        CodeDef dateBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "date");
        PopulationDef popDef =
                new PopulationDef("pop-1", null, MeasurePopulationType.DENOMINATOR, "Denominator", dateBasis, null);

        // Add date values for subjects
        popDef.addResource("Patient/1", "2024-01-01");
        popDef.addResource("Patient/2", "2024-01-02");
        popDef.addResource("Patient/3", "2024-01-01"); // Duplicate date value

        assertFalse(popDef.isBooleanBasis());
        assertEquals(3, popDef.getCount(), "Date basis should count all resources");
        assertEquals(3, popDef.getSubjects().size());
    }

    /**
     * Test getCount() for MEASUREOBSERVATION type - counts observations.
     */
    @Test
    void testGetCount_MeasureObservation_CountsObservations() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef = new PopulationDef(
                "pop-obs", null, MeasurePopulationType.MEASUREOBSERVATION, "MeasureObservation", booleanBasis, null);

        // Add observations (Maps) for subjects
        // Each observation is a Map with key-value pairs
        Map<String, Object> obs1 = Map.of("value", 10.0);
        Map<String, Object> obs2 = Map.of("value", 20.0, "unit", "mg");
        Map<String, Object> obs3 = Map.of("value", 30.0);

        popDef.addResource("Patient/1", obs1);
        popDef.addResource("Patient/2", obs2);
        popDef.addResource("Patient/3", obs3);

        // For MEASUREOBSERVATION, getCount() should count subjects
        assertEquals(3, popDef.getCount(), "MEASUREOBSERVATION should count observation entries");
    }

    /**
     * Test removeExcludedMeasureObservationResource with boolean basis.
     * When all entries are removed from a subject's inner map, that subject should not
     * be counted in getCount().
     */
    @Test
    void testRemoveExcludedMeasureObservationResource_BooleanBasis_RemovesEmptyMaps() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef = new PopulationDef(
                "pop-obs",
                null,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                booleanBasis,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.SUM,
                null);

        // Create observation maps with encounters as keys
        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3 = (Encounter) new Encounter().setId("Encounter/3");

        // Add observations for three subjects
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));
        popDef.addResource("Patient/1", obsMap1);

        Map<Object, Object> obsMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap2.put(enc2, new QuantityDef(200.0));
        popDef.addResource("Patient/2", obsMap2);

        Map<Object, Object> obsMap3 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap3.put(enc3, new QuantityDef(300.0));
        popDef.addResource("Patient/3", obsMap3);

        // Initial count should be 3
        assertEquals(3, popDef.getCount(), "Should have 3 observations before removal");
        assertEquals(3, popDef.getSubjects().size(), "Should have 3 subjects");

        // Remove enc1 from Patient/1 - this should empty Patient/1's inner map
        popDef.removeExcludedMeasureObservationResource("Patient/1", enc1);

        // After removal, Patient/1's inner map should be empty and purged
        // Count should now be 2 (only Patient/2 and Patient/3 remain)
        assertEquals(2, popDef.getCount(), "Should have 2 observations after removing Patient/1's only entry");
        assertEquals(2, popDef.getSubjects().size(), "Should have 2 subjects after empty map removal");

        // Patient/1 should no longer be in the subjects
        assertFalse(popDef.getSubjects().contains("Patient/1"), "Patient/1 should not be counted after removal");
        assertTrue(popDef.getSubjects().contains("Patient/2"), "Patient/2 should still be counted");
        assertTrue(popDef.getSubjects().contains("Patient/3"), "Patient/3 should still be counted");
    }

    /**
     * Test removeExcludedMeasureObservationResource with encounter basis and multiple entries.
     * When one entry is removed from a subject's inner map but others remain, the subject
     * should still be counted.
     */
    @Test
    void testRemoveExcludedMeasureObservationResource_EncounterBasis_PartialRemoval() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef = new PopulationDef(
                "pop-obs",
                null,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                encounterBasis,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.SUM,
                null);

        // Create observation maps with multiple encounters for one subject
        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3 = (Encounter) new Encounter().setId("Encounter/3");

        Map<Object, Object> obsMapSubject1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMapSubject1.put(enc1, new QuantityDef(100.0));
        obsMapSubject1.put(enc2, new QuantityDef(200.0));
        popDef.addResource("Patient/1", obsMapSubject1);

        Map<Object, Object> obsMapSubject2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMapSubject2.put(enc3, new QuantityDef(300.0));
        popDef.addResource("Patient/2", obsMapSubject2);

        // Initial: Patient/1 has 2 entries, Patient/2 has 1 entry = 3 total
        assertEquals(3, popDef.getCount(), "Should have 3 observations before removal");

        // Remove one entry from Patient/1's map (enc1)
        popDef.removeExcludedMeasureObservationResource("Patient/1", enc1);

        // Patient/1's map should still have 1 entry (enc2), so count should be 2
        assertEquals(2, popDef.getCount(), "Should have 2 observations after removing one entry");
        assertEquals(2, popDef.getSubjects().size(), "Should still have 2 subjects");

        // Both subjects should still be present
        assertTrue(popDef.getSubjects().contains("Patient/1"), "Patient/1 should still be counted");
        assertTrue(popDef.getSubjects().contains("Patient/2"), "Patient/2 should still be counted");
    }

    /**
     * Test removeExcludedMeasureObservationResource removes all entries from subject's map.
     * This simulates the scenario where all of a subject's observations are excluded.
     */
    @Test
    void testRemoveExcludedMeasureObservationResource_RemovesAllEntries_SubjectNotCounted() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef = new PopulationDef(
                "pop-obs",
                null,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                encounterBasis,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.COUNT,
                null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");

        // Patient/1 has 2 observations
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));
        obsMap1.put(enc2, new QuantityDef(200.0));
        popDef.addResource("Patient/1", obsMap1);

        assertEquals(2, popDef.getCount(), "Should have 2 observations initially");
        assertEquals(1, popDef.getSubjects().size(), "Should have 1 subject");

        // Remove first entry
        popDef.removeExcludedMeasureObservationResource("Patient/1", enc1);
        assertEquals(1, popDef.getCount(), "Should have 1 observation after first removal");
        assertEquals(1, popDef.getSubjects().size(), "Should still have 1 subject");

        // Remove second entry - now the inner map should be empty and purged
        popDef.removeExcludedMeasureObservationResource("Patient/1", enc2);
        assertEquals(0, popDef.getCount(), "Should have 0 observations after removing all entries");
        assertEquals(0, popDef.getSubjects().size(), "Should have 0 subjects after empty map removal");

        assertFalse(
                popDef.getSubjects().contains("Patient/1"),
                "Patient/1 should not be counted after all observations removed");
    }

    /**
     * Test retainAllSubjects() - keeps only subjects that exist in both populations.
     */
    @Test
    void testRetainAllSubjects_WithCommonSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 =
                new PopulationDef("pop-2", null, MeasurePopulationType.DENOMINATOR, "Denominator", booleanBasis, null);

        // popDef1 has subjects 1, 2, 3
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);
        popDef1.addResource("Patient/3", true);

        // popDef2 has subjects 2, 3, 4
        popDef2.addResource("Patient/2", true);
        popDef2.addResource("Patient/3", true);
        popDef2.addResource("Patient/4", true);

        // Retain only common subjects (2 and 3)
        popDef1.retainAllSubjects(popDef2);

        assertEquals(2, popDef1.getSubjects().size(), "Should have 2 common subjects");
        assertFalse(popDef1.getSubjects().contains("Patient/1"), "Patient/1 should be removed");
        assertTrue(popDef1.getSubjects().contains("Patient/2"), "Patient/2 should be retained");
        assertTrue(popDef1.getSubjects().contains("Patient/3"), "Patient/3 should be retained");
        assertFalse(popDef1.getSubjects().contains("Patient/4"), "Patient/4 was not in popDef1");
    }

    /**
     * Test retainAllSubjects() - when no subjects are common.
     */
    @Test
    void testRetainAllSubjects_WithNoCommonSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 =
                new PopulationDef("pop-2", null, MeasurePopulationType.DENOMINATOR, "Denominator", booleanBasis, null);

        // popDef1 has subjects 1, 2
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);

        // popDef2 has subjects 3, 4
        popDef2.addResource("Patient/3", true);
        popDef2.addResource("Patient/4", true);

        // Retain only common subjects (none)
        popDef1.retainAllSubjects(popDef2);

        assertEquals(0, popDef1.getSubjects().size(), "Should have no subjects after retention");
    }

    /**
     * Test retainAllSubjects() - when all subjects are common.
     */
    @Test
    void testRetainAllSubjects_WithAllSubjectsCommon() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 =
                new PopulationDef("pop-2", null, MeasurePopulationType.DENOMINATOR, "Denominator", booleanBasis, null);

        // Both have the same subjects
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);

        popDef2.addResource("Patient/1", true);
        popDef2.addResource("Patient/2", true);

        popDef1.retainAllSubjects(popDef2);

        assertEquals(2, popDef1.getSubjects().size(), "Should still have 2 subjects");
        assertTrue(popDef1.getSubjects().contains("Patient/1"));
        assertTrue(popDef1.getSubjects().contains("Patient/2"));
    }

    /**
     * Test removeAllResources() - removes resources that exist in both populations for a subject.
     */
    @Test
    void testRemoveAllResources_WithCommonResources() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2",
                null,
                MeasurePopulationType.DENOMINATOREXCLUSION,
                "DenominatorExclusion",
                encounterBasis,
                null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3 = (Encounter) new Encounter().setId("Encounter/3");

        // popDef1 Patient/1 has encounters 1, 2, 3
        popDef1.addResource("Patient/1", enc1);
        popDef1.addResource("Patient/1", enc2);
        popDef1.addResource("Patient/1", enc3);

        // popDef2 Patient/1 has encounters 2, 3 (exclusions)
        Encounter enc2b = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3b = (Encounter) new Encounter().setId("Encounter/3");
        popDef2.addResource("Patient/1", enc2b);
        popDef2.addResource("Patient/1", enc3b);

        // Remove encounters that are in both (2 and 3)
        popDef1.removeAllResources("Patient/1", popDef2);

        assertEquals(1, popDef1.getResourcesForSubject("Patient/1").size(), "Should have 1 resource remaining");
        assertTrue(
                getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1), "Encounter/1 should remain in popDef1");
    }

    /**
     * Test removeAllResources() - when no resources are common.
     */
    @Test
    void testRemoveAllResources_WithNoCommonResources() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2",
                null,
                MeasurePopulationType.DENOMINATOREXCLUSION,
                "DenominatorExclusion",
                encounterBasis,
                null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");

        // popDef1 Patient/1 has encounter 1
        popDef1.addResource("Patient/1", enc1);

        // popDef2 Patient/1 has encounter 2
        popDef2.addResource("Patient/1", enc2);

        // Remove resources that are in both (none)
        popDef1.removeAllResources("Patient/1", popDef2);

        assertEquals(1, popDef1.getResourcesForSubject("Patient/1").size(), "Should still have 1 resource");
        assertTrue(getResourcesDistinctAcrossAllSubjects(popDef1).contains(enc1), "Encounter/1 should remain");
    }

    /**
     * Test removeAllResources() - when all resources are common.
     */
    @Test
    void testRemoveAllResources_WithAllResourcesCommon() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2",
                null,
                MeasurePopulationType.DENOMINATOREXCLUSION,
                "DenominatorExclusion",
                encounterBasis,
                null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");

        // Both have the same encounters for Patient/1
        popDef1.addResource("Patient/1", enc1);
        popDef1.addResource("Patient/1", enc2);

        Encounter enc1b = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2b = (Encounter) new Encounter().setId("Encounter/2");
        popDef2.addResource("Patient/1", enc1b);
        popDef2.addResource("Patient/1", enc2b);

        // Remove all common resources
        popDef1.removeAllResources("Patient/1", popDef2);

        assertEquals(0, popDef1.getResourcesForSubject("Patient/1").size(), "Should have no resources remaining");
    }

    /**
     * Test removeAllSubjects() - removes subjects that exist in both populations.
     */
    @Test
    void testRemoveAllSubjects_WithCommonSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2", null, MeasurePopulationType.DENOMINATOREXCLUSION, "DenominatorExclusion", booleanBasis, null);

        // popDef1 has subjects 1, 2, 3
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);
        popDef1.addResource("Patient/3", true);

        // popDef2 has subjects 2, 3 (exclusions)
        popDef2.addResource("Patient/2", true);
        popDef2.addResource("Patient/3", true);

        // Remove subjects that are in both (2 and 3)
        popDef1.removeAllSubjects(popDef2);

        assertEquals(1, popDef1.getSubjects().size(), "Should have 1 subject remaining");
        assertTrue(popDef1.getSubjects().contains("Patient/1"), "Patient/1 should remain");
        assertFalse(popDef1.getSubjects().contains("Patient/2"), "Patient/2 should be removed");
        assertFalse(popDef1.getSubjects().contains("Patient/3"), "Patient/3 should be removed");
    }

    /**
     * Test removeAllSubjects() - when no subjects are common.
     */
    @Test
    void testRemoveAllSubjects_WithNoCommonSubjects() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2", null, MeasurePopulationType.DENOMINATOREXCLUSION, "DenominatorExclusion", booleanBasis, null);

        // popDef1 has subjects 1, 2
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);

        // popDef2 has subjects 3, 4
        popDef2.addResource("Patient/3", true);
        popDef2.addResource("Patient/4", true);

        // Remove subjects that are in both (none)
        popDef1.removeAllSubjects(popDef2);

        assertEquals(2, popDef1.getSubjects().size(), "Should still have 2 subjects");
        assertTrue(popDef1.getSubjects().contains("Patient/1"));
        assertTrue(popDef1.getSubjects().contains("Patient/2"));
    }

    /**
     * Test removeAllSubjects() - when all subjects are common.
     */
    @Test
    void testRemoveAllSubjects_WithAllSubjectsCommon() {
        CodeDef booleanBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef popDef1 = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", booleanBasis, null);
        PopulationDef popDef2 = new PopulationDef(
                "pop-2", null, MeasurePopulationType.DENOMINATOREXCLUSION, "DenominatorExclusion", booleanBasis, null);

        // Both have the same subjects
        popDef1.addResource("Patient/1", true);
        popDef1.addResource("Patient/2", true);

        popDef2.addResource("Patient/1", true);
        popDef2.addResource("Patient/2", true);

        // Remove all common subjects
        popDef1.removeAllSubjects(popDef2);

        assertEquals(0, popDef1.getSubjects().size(), "Should have no subjects remaining");
    }

    /**
     * Test getAllSubjectResources() with multiple subjects and resources.
     */
    @Test
    void testGetAllSubjectResources_MultipleSubjectsAndResources() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef = new PopulationDef(
                "pop-1", null, MeasurePopulationType.INITIALPOPULATION, "InitialPopulation", encounterBasis, null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3 = (Encounter) new Encounter().setId("Encounter/3");

        popDef.addResource("Patient/1", enc1);
        popDef.addResource("Patient/1", enc2);
        popDef.addResource("Patient/2", enc3);

        assertEquals(3, popDef.getAllSubjectResources().size(), "Should have 3 total resources");
    }

    /**
     * Test addResource() with same value for different subjects.
     */
    @Test
    void testAddResource_SameValueDifferentSubjects() {
        CodeDef stringBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "String");
        PopulationDef popDef =
                new PopulationDef("pop-1", null, MeasurePopulationType.NUMERATOR, "Numerator", stringBasis, null);

        popDef.addResource("Patient/1", "common-value");
        popDef.addResource("Patient/2", "common-value");

        assertEquals(2, popDef.getSubjects().size(), "Should have 2 subjects");
        assertEquals(
                2,
                popDef.getAllSubjectResources().size(),
                "Should count both occurrences of same value for different subjects");
    }

    /**
     * Test countObservations() with multiple observation maps.
     */
    @Test
    void testCountObservations_WithMultipleMaps() {
        CodeDef encounterBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "Encounter");
        PopulationDef popDef = new PopulationDef(
                "pop-obs",
                null,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                encounterBasis,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.SUM,
                null);

        Encounter enc1 = (Encounter) new Encounter().setId("Encounter/1");
        Encounter enc2 = (Encounter) new Encounter().setId("Encounter/2");
        Encounter enc3 = (Encounter) new Encounter().setId("Encounter/3");

        // Patient/1 has 2 observations
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));
        obsMap1.put(enc2, new QuantityDef(200.0));
        popDef.addResource("Patient/1", obsMap1);

        // Patient/2 has 1 observation
        Map<Object, Object> obsMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap2.put(enc3, new QuantityDef(300.0));
        popDef.addResource("Patient/2", obsMap2);

        assertEquals(3, popDef.countObservations(), "Should count all observation entries across all maps");
        assertEquals(3, popDef.getCount(), "getCount() should match countObservations() for MEASUREOBSERVATION");
    }
}
