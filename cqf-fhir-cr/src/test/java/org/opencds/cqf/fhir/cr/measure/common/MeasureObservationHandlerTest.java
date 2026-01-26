package org.opencds.cqf.fhir.cr.measure.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MeasureObservationHandler class.
 * Focuses on testing the core observation handling logic, particularly
 * removeObservationResourcesInPopulation() which handles exclusions.
 */
class MeasureObservationHandlerTest {

    private static final String SUBJECT_ID_1 = "patient-1";
    private static final String SUBJECT_ID_2 = "patient-2";
    private static final String SUBJECT_ID_3 = "patient-3";

    private PopulationDef measureObservationDef;
    private PopulationDef measurePopulationExclusionDef;
    private final ConceptDef conceptDef =
            new ConceptDef(List.of(new CodeDef("http://test.system", "test-code")), "Test Concept");
    private final CodeDef codeDef = new CodeDef("http://hl7.org/fhir/resource-types", "Encounter");

    /**
     * Tests that removeObservationResourcesInPopulation removes observation entries
     * when matching exclusion resources are found, using separate JVM object instances
     * with identical IDs.
     */
    @Test
    void removeObservationResourcesInPopulation_removesMatchingResources_withSeparateObjectInstances() {
        // Given: Create separate Encounter instances with identical IDs
        Encounter encounter1InObservation = new Encounter();
        encounter1InObservation.setId("encounter-1");

        Encounter encounter1InExclusion = new Encounter();
        encounter1InExclusion.setId("encounter-1");

        Encounter encounter2InObservation = new Encounter();
        encounter2InObservation.setId("encounter-2");

        // Verify they are separate objects
        assertNotSame(encounter1InObservation, encounter1InExclusion, "Encounter objects should be separate instances");
        assertEquals(
                encounter1InObservation.getIdElement(),
                encounter1InExclusion.getIdElement(),
                "Encounter IDs should be equal");

        // Create measure observation map: Map<Encounter, QuantityDef>
        Map<Object, Object> observationMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        observationMap1.put(encounter1InObservation, new QuantityDef(120.0));

        Map<Object, Object> observationMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        observationMap2.put(encounter2InObservation, new QuantityDef(180.0));

        // Create MEASUREOBSERVATION population with the maps
        measureObservationDef = new PopulationDef(
                "measure-observation",
                conceptDef,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                codeDef,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.SUM,
                List.of());

        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(observationMap1);
        observationResources.add(observationMap2);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        // Create MEASUREPOPULATIONEXCLUSION population with encounter to exclude
        measurePopulationExclusionDef = new PopulationDef(
                "measure-population-exclusion",
                conceptDef,
                MeasurePopulationType.MEASUREPOPULATIONEXCLUSION,
                "MeasurePopulationExclusion",
                codeDef,
                List.of());

        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(encounter1InExclusion); // This should match encounter1InObservation by ID
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When: Remove observation resources that match exclusions
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: The empty map should be removed, leaving only 1 map
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat(
                "Should have 1 observation map remaining after empty map removal", remainingObservations, hasSize(1));

        // Verify encounter-1's map was removed and only encounter-2 remains
        Map<?, ?> remainingMap = (Map<?, ?>) remainingObservations.iterator().next();
        assertThat("Remaining map should have 1 entry", remainingMap.size(), is(1));
        assertTrue(remainingMap.containsKey(encounter2InObservation), "Should contain encounter-2");
        var quantityFromMapForEncounter = remainingMap.get(encounter2InObservation);
        assertInstanceOf(QuantityDef.class, quantityFromMapForEncounter);
        var quantityFromMap = (QuantityDef) quantityFromMapForEncounter;
        assertQuantityEquals(180.0, quantityFromMap, "Encounter-2 should have correct quantity");
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles multiple exclusions correctly.
     */
    @Test
    void removeObservationResourcesInPopulation_removesMultipleMatchingResources() {
        // Given: Three encounters in observations, two in exclusions
        Encounter enc1Obs = createEncounter("encounter-1");
        Encounter enc2Obs = createEncounter("encounter-2");
        Encounter enc3Obs = createEncounter("encounter-3");

        Encounter enc1Excl = createEncounter("encounter-1");
        Encounter enc2Excl = createEncounter("encounter-2");

        // Create observation maps
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1Obs, new QuantityDef(100.0));

        Map<Object, Object> obsMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap2.put(enc2Obs, new QuantityDef(200.0));

        Map<Object, Object> obsMap3 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap3.put(enc3Obs, new QuantityDef(300.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap1);
        observationResources.add(obsMap2);
        observationResources.add(obsMap3);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        // Create exclusions
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(enc1Excl);
        exclusionResources.add(enc2Excl);
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: The two empty maps (obsMap1 and obsMap2) should be removed, leaving only obsMap3
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));

        // Verify only enc3 remains
        Map<?, ?> remainingMap = (Map<?, ?>) remainingObservations.iterator().next();
        assertThat("Map should have 1 entry", remainingMap.size(), is(1));
        assertTrue(remainingMap.containsKey(enc3Obs), "Should contain encounter-3");
        var quantityFromMapForEncounter = remainingMap.get(enc3Obs);
        assertInstanceOf(QuantityDef.class, quantityFromMapForEncounter);
        var quantityFromMap = (QuantityDef) quantityFromMapForEncounter;

        assertQuantityEquals(300.0, quantityFromMap, "Encounter-3 should have correct quantity");
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles case where no exclusions match.
     */
    @Test
    void removeObservationResourcesInPopulation_noMatchingExclusions_allObservationsRemain() {
        // Given: Observations with different encounters than exclusions
        Encounter enc1Obs = createEncounter("encounter-1");
        Encounter enc2Obs = createEncounter("encounter-2");
        Encounter enc3Excl = createEncounter("encounter-3");
        Encounter enc4Excl = createEncounter("encounter-4");

        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1Obs, new QuantityDef(100.0));

        Map<Object, Object> obsMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap2.put(enc2Obs, new QuantityDef(200.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap1);
        observationResources.add(obsMap2);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(enc3Excl);
        exclusionResources.add(enc4Excl);
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 2 observation maps remaining", remainingObservations, hasSize(2));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles empty exclusion set gracefully.
     */
    @Test
    void removeObservationResourcesInPopulation_emptyExclusions_allObservationsRemain() {
        // Given: Observations but no exclusions
        Encounter enc1 = createEncounter("encounter-1");
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap1);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, new HashSetForFhirResourcesAndCqlTypes<>());

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles null measureObservationDef gracefully.
     */
    @Test
    void removeObservationResourcesInPopulation_nullMeasureObservation_noExceptionThrown() {
        // Given: Null measure observation def
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(createEncounter("encounter-1"));
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When/Then: Should not throw exception
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, null);
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles null measurePopulationExclusionDef gracefully.
     */
    @Test
    void removeObservationResourcesInPopulation_nullExclusionDef_noExceptionThrown() {
        // Given: Null exclusion def
        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(createEncounter("encounter-1"), new QuantityDef(100.0));
        observationResources.add(obsMap1);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        // When/Then: Should not throw exception, all observations remain
        MeasureObservationHandler.removeObservationResourcesInPopulation(SUBJECT_ID_1, null, measureObservationDef);

        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles subject with no exclusion resources.
     */
    @Test
    void removeObservationResourcesInPopulation_subjectNotInExclusions_allObservationsRemain() {
        // Given: Observations for subject-1, exclusions for different subject
        Encounter enc1 = createEncounter("encounter-1");
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap1);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(createEncounter("encounter-1"));
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_2, exclusionResources);

        // When: Try to remove for subject-1 (no exclusions)
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));
    }

    /**
     * Tests that removeObservationResourcesInPopulation correctly handles case where
     * patient has one cancelled encounter and one non-cancelled encounter, and only
     * the cancelled encounter should be removed from observations.
     */
    @Test
    void removeObservationResourcesInPopulation_mixedCancelledAndNonCancelled_onlyCancelledRemoved() {
        // Given: Patient-3 scenario from integration test
        // - Has 2 encounters: one cancelled (should be excluded), one non-cancelled
        Encounter cancelledEncObs = createEncounter("patient-3-encounter-cancelled");
        Encounter nonCancelledEncObs = createEncounter("patient-3-encounter-active");

        Encounter cancelledEncExcl = createEncounter("patient-3-encounter-cancelled");

        // Create observation maps for both encounters
        Map<Object, Object> obsMapCancelled = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMapCancelled.put(cancelledEncObs, new QuantityDef(100.0));

        Map<Object, Object> obsMapActive = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMapActive.put(nonCancelledEncObs, new QuantityDef(420.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMapCancelled);
        observationResources.add(obsMapActive);
        measureObservationDef.subjectResources.put(SUBJECT_ID_3, observationResources);

        // Only the cancelled encounter is in exclusions
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(cancelledEncExcl);
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_3, exclusionResources);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_3, measurePopulationExclusionDef, measureObservationDef);

        // Then: The empty obsMapCancelled should be removed, leaving only obsMapActive
        Set<Object> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_3);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));

        // Verify only the non-cancelled encounter remains
        Map<?, ?> remainingMap = (Map<?, ?>) remainingObservations.iterator().next();
        assertThat("Map should have 1 entry", remainingMap.size(), is(1));
        assertTrue(remainingMap.containsKey(nonCancelledEncObs), "Should contain non-cancelled encounter");
        QuantityDef activeQuantity = (QuantityDef) remainingMap.get(nonCancelledEncObs);
        assertNotNull(activeQuantity);
        assertThat("Active encounter should have correct quantity", activeQuantity.value(), is(closeTo(420.0, 0.01)));
    }

    /**
     * Tests that when all observation entries for a subject are removed, the subject is no longer
     * counted. This validates the fix for empty inner map removal.
     */
    @Test
    void removeObservationResourcesInPopulation_allEntriesRemoved_subjectNotCounted() {
        // Given: One subject with one observation entry that will be excluded
        Encounter enc1Obs = createEncounter("encounter-1");
        Encounter enc1Excl = createEncounter("encounter-1");

        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1Obs, new QuantityDef(100.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap1);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        // Initial count should include the subject
        assertEquals(1, measureObservationDef.getCount(), "Initial count should be 1");
        assertEquals(1, measureObservationDef.getSubjects().size(), "Should have 1 subject");

        // Create exclusion with the same encounter
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(enc1Excl);
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When: Remove observation resources
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: Subject should no longer be counted (empty map should be removed)
        assertEquals(0, measureObservationDef.getCount(), "Count should be 0 after removing all entries");
        assertEquals(0, measureObservationDef.getSubjects().size(), "Should have 0 subjects after empty map removal");
        assertFalse(
                measureObservationDef.getSubjects().contains(SUBJECT_ID_1),
                "Subject should not be counted after all observations removed");
    }

    /**
     * Tests that when some but not all observation entries for a subject are removed,
     * the subject is still counted.
     */
    @Test
    void removeObservationResourcesInPopulation_partialRemoval_subjectStillCounted() {
        // Given: One subject with two observation entries, only one will be excluded
        Encounter enc1 = createEncounter("encounter-1");
        Encounter enc2 = createEncounter("encounter-2");
        Encounter enc1Excl = createEncounter("encounter-1");

        Map<Object, Object> obsMap = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap.put(enc1, new QuantityDef(100.0));
        obsMap.put(enc2, new QuantityDef(200.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> observationResources = new HashSetForFhirResourcesAndCqlTypes<>();
        observationResources.add(obsMap);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, observationResources);

        // Initial count should be 2 (two observation entries)
        assertEquals(2, measureObservationDef.getCount(), "Initial count should be 2");
        assertEquals(1, measureObservationDef.getSubjects().size(), "Should have 1 subject");

        // Create exclusion with only one encounter
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        Set<Object> exclusionResources = new HashSetForFhirResourcesAndCqlTypes<>();
        exclusionResources.add(enc1Excl);
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, exclusionResources);

        // When: Remove observation resources
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: Subject should still be counted (one entry remains)
        assertEquals(1, measureObservationDef.getCount(), "Count should be 1 after removing one entry");
        assertEquals(1, measureObservationDef.getSubjects().size(), "Should still have 1 subject");
        assertTrue(
                measureObservationDef.getSubjects().contains(SUBJECT_ID_1),
                "Subject should still be counted with remaining observations");
    }

    /**
     * Tests that multiple subjects are handled correctly - some completely excluded, some partially.
     */
    @Test
    void removeObservationResourcesInPopulation_multipleSubjects_countsCorrectly() {
        // Given: Three subjects with different exclusion scenarios
        // Subject 1: All entries excluded (1 encounter)
        Encounter enc1 = createEncounter("encounter-1");
        Map<Object, Object> obsMap1 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap1.put(enc1, new QuantityDef(100.0));

        // Subject 2: Partial exclusion (2 encounters, 1 excluded)
        Encounter enc2 = createEncounter("encounter-2");
        Encounter enc3 = createEncounter("encounter-3");
        Map<Object, Object> obsMap2 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap2.put(enc2, new QuantityDef(200.0));
        obsMap2.put(enc3, new QuantityDef(300.0));

        // Subject 3: No exclusions (1 encounter)
        Encounter enc4 = createEncounter("encounter-4");
        Map<Object, Object> obsMap3 = new HashMapForFhirResourcesAndCqlTypes<>();
        obsMap3.put(enc4, new QuantityDef(400.0));

        measureObservationDef = createMeasureObservationDef();
        Set<Object> obs1 = new HashSetForFhirResourcesAndCqlTypes<>();
        obs1.add(obsMap1);
        measureObservationDef.subjectResources.put(SUBJECT_ID_1, obs1);

        Set<Object> obs2 = new HashSetForFhirResourcesAndCqlTypes<>();
        obs2.add(obsMap2);
        measureObservationDef.subjectResources.put(SUBJECT_ID_2, obs2);

        Set<Object> obs3 = new HashSetForFhirResourcesAndCqlTypes<>();
        obs3.add(obsMap3);
        measureObservationDef.subjectResources.put(SUBJECT_ID_3, obs3);

        // Initial: 1 + 2 + 1 = 4 total observations, 3 subjects
        assertEquals(4, measureObservationDef.getCount(), "Initial count should be 4");
        assertEquals(3, measureObservationDef.getSubjects().size(), "Should have 3 subjects");

        // Create exclusions for subjects 1 and 2
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();

        // Subject 1: Exclude all (enc1)
        Set<Object> excl1 = new HashSetForFhirResourcesAndCqlTypes<>();
        excl1.add(createEncounter("encounter-1"));
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_1, excl1);

        // Subject 2: Exclude one (enc2)
        Set<Object> excl2 = new HashSetForFhirResourcesAndCqlTypes<>();
        excl2.add(createEncounter("encounter-2"));
        measurePopulationExclusionDef.subjectResources.put(SUBJECT_ID_2, excl2);

        // When: Remove for each subject
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_2, measurePopulationExclusionDef, measureObservationDef);
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_3, measurePopulationExclusionDef, measureObservationDef);

        // Then: Subject 1 fully excluded (0), Subject 2 partial (1), Subject 3 unchanged (1) = 2 total
        // Subject 1 should be removed completely
        assertEquals(2, measureObservationDef.getCount(), "Count should be 2 after removals");
        assertEquals(2, measureObservationDef.getSubjects().size(), "Should have 2 subjects remaining");
        assertFalse(measureObservationDef.getSubjects().contains(SUBJECT_ID_1), "Subject 1 should not be counted");
        assertTrue(measureObservationDef.getSubjects().contains(SUBJECT_ID_2), "Subject 2 should still be counted");
        assertTrue(measureObservationDef.getSubjects().contains(SUBJECT_ID_3), "Subject 3 should still be counted");
    }

    // Helper methods

    private Encounter createEncounter(String id) {
        Encounter encounter = new Encounter();
        encounter.setId(id);
        return encounter;
    }

    private PopulationDef createMeasureObservationDef() {
        return new PopulationDef(
                "measure-observation",
                conceptDef,
                MeasurePopulationType.MEASUREOBSERVATION,
                "MeasureObservation",
                codeDef,
                "measure-population",
                ContinuousVariableObservationAggregateMethod.SUM,
                List.of());
    }

    private PopulationDef createMeasurePopulationExclusionDef() {
        return new PopulationDef(
                "measure-population-exclusion",
                conceptDef,
                MeasurePopulationType.MEASUREPOPULATIONEXCLUSION,
                "MeasurePopulationExclusion",
                codeDef,
                List.of());
    }

    private void assertQuantityEquals(double expectedDouble, QuantityDef quantityFromMap, String error) {

        final Double value = quantityFromMap.value();

        assertNotNull(value, error);

        assertEquals(expectedDouble, value, 0.001, error);
    }
}
