package org.opencds.cqf.fhir.cr.measure.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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

        // Create observation accumulators
        var observationMap1 = new ObservationAccumulator(
                List.of(new ObservationEntry(encounter1InObservation, new QuantityDef(120.0))));
        var observationMap2 = new ObservationAccumulator(
                List.of(new ObservationEntry(encounter2InObservation, new QuantityDef(180.0))));

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

        measureObservationDef.addResource(SUBJECT_ID_1, observationMap1);
        measureObservationDef.addResource(SUBJECT_ID_1, observationMap2);

        // Create MEASUREPOPULATIONEXCLUSION population with encounter to exclude
        measurePopulationExclusionDef = new PopulationDef(
                "measure-population-exclusion",
                conceptDef,
                MeasurePopulationType.MEASUREPOPULATIONEXCLUSION,
                "MeasurePopulationExclusion",
                codeDef,
                List.of());

        // This encounter should match encounter1InObservation by ID
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, encounter1InExclusion);

        // When: Remove observation resources that match exclusions
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: The empty map should be removed, leaving only 1 map
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat(
                "Should have 1 observation map remaining after empty map removal", remainingObservations, hasSize(1));

        // Verify encounter-1's entry was removed and only encounter-2 remains
        List<ObservationEntry> remainingEntries = remainingObservations
                .iterator()
                .next()
                .asObservationAccumulator()
                .orElseThrow()
                .entries();
        assertThat("Remaining accumulator should have 1 entry", remainingEntries, hasSize(1));
        ObservationEntry remainingEntry = remainingEntries.get(0);
        assertTrue(
                FhirResourceAndCqlTypeUtils.areObjectsEqual(remainingEntry.inputResource(), encounter2InObservation),
                "Should contain encounter-2");
        assertNotNull(remainingEntry.observation());
        assertQuantityEquals(180.0, remainingEntry.observation(), "Encounter-2 should have correct quantity");
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

        // Create observation accumulators
        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1Obs, new QuantityDef(100.0))));
        var obsMap2 = new ObservationAccumulator(List.of(new ObservationEntry(enc2Obs, new QuantityDef(200.0))));
        var obsMap3 = new ObservationAccumulator(List.of(new ObservationEntry(enc3Obs, new QuantityDef(300.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap2);
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap3);

        // Create exclusions
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc1Excl);
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc2Excl);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: The two empty maps (obsMap1 and obsMap2) should be removed, leaving only obsMap3
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));

        // Verify only enc3 remains
        List<ObservationEntry> remainingEntries = remainingObservations
                .iterator()
                .next()
                .asObservationAccumulator()
                .orElseThrow()
                .entries();
        assertThat("Accumulator should have 1 entry", remainingEntries, hasSize(1));
        ObservationEntry remainingEntry = remainingEntries.get(0);
        assertTrue(
                FhirResourceAndCqlTypeUtils.areObjectsEqual(remainingEntry.inputResource(), enc3Obs),
                "Should contain encounter-3");
        assertNotNull(remainingEntry.observation());
        assertQuantityEquals(300.0, remainingEntry.observation(), "Encounter-3 should have correct quantity");
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

        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1Obs, new QuantityDef(100.0))));
        var obsMap2 = new ObservationAccumulator(List.of(new ObservationEntry(enc2Obs, new QuantityDef(200.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap2);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc3Excl);
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc4Excl);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 2 observation maps remaining", remainingObservations, hasSize(2));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles empty exclusion set gracefully.
     */
    @Test
    void removeObservationResourcesInPopulation_emptyExclusions_allObservationsRemain() {
        // Given: Observations but no exclusions
        Encounter enc1 = createEncounter("encounter-1");
        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1, new QuantityDef(100.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        // No exclusions for this subject — getResourcesForSubject returns an empty default set

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles null measureObservationDef gracefully.
     */
    @Test
    void removeObservationResourcesInPopulation_nullMeasureObservation_noExceptionThrown() {
        // Given: Null measure observation def
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, createEncounter("encounter-1"));

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
        var obsMap1 = new ObservationAccumulator(
                List.of(new ObservationEntry(createEncounter("encounter-1"), new QuantityDef(100.0))));
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);

        // When/Then: Should not throw exception, all observations remain
        MeasureObservationHandler.removeObservationResourcesInPopulation(SUBJECT_ID_1, null, measureObservationDef);

        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));
    }

    /**
     * Tests that removeObservationResourcesInPopulation handles subject with no exclusion resources.
     */
    @Test
    void removeObservationResourcesInPopulation_subjectNotInExclusions_allObservationsRemain() {
        // Given: Observations for subject-1, exclusions for different subject
        Encounter enc1 = createEncounter("encounter-1");
        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1, new QuantityDef(100.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);

        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_2, createEncounter("encounter-1"));

        // When: Try to remove for subject-1 (no exclusions)
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_1, measurePopulationExclusionDef, measureObservationDef);

        // Then: All observations should remain
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_1);
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

        // Create observation accumulators for both encounters
        var obsMapCancelled =
                new ObservationAccumulator(List.of(new ObservationEntry(cancelledEncObs, new QuantityDef(100.0))));
        var obsMapActive =
                new ObservationAccumulator(List.of(new ObservationEntry(nonCancelledEncObs, new QuantityDef(420.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_3, obsMapCancelled);
        measureObservationDef.addResource(SUBJECT_ID_3, obsMapActive);

        // Only the cancelled encounter is in exclusions
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_3, cancelledEncExcl);

        // When
        MeasureObservationHandler.removeObservationResourcesInPopulation(
                SUBJECT_ID_3, measurePopulationExclusionDef, measureObservationDef);

        // Then: The empty obsMapCancelled should be removed, leaving only obsMapActive
        Set<CqlExpressionValue> remainingObservations = measureObservationDef.getResourcesForSubject(SUBJECT_ID_3);
        assertThat("Should have 1 observation map remaining", remainingObservations, hasSize(1));

        // Verify only the non-cancelled encounter remains
        List<ObservationEntry> remainingEntries = remainingObservations
                .iterator()
                .next()
                .asObservationAccumulator()
                .orElseThrow()
                .entries();
        assertThat("Accumulator should have 1 entry", remainingEntries, hasSize(1));
        ObservationEntry remainingEntry = remainingEntries.get(0);
        assertTrue(
                FhirResourceAndCqlTypeUtils.areObjectsEqual(remainingEntry.inputResource(), nonCancelledEncObs),
                "Should contain non-cancelled encounter");
        QuantityDef activeQuantity = remainingEntry.observation();
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

        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1Obs, new QuantityDef(100.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);

        // Initial count should include the subject
        assertEquals(1, measureObservationDef.getCount(), "Initial count should be 1");
        assertEquals(1, measureObservationDef.getSubjects().size(), "Should have 1 subject");

        // Create exclusion with the same encounter
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc1Excl);

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

        var obsMap = new ObservationAccumulator(List.of(
                new ObservationEntry(enc1, new QuantityDef(100.0)),
                new ObservationEntry(enc2, new QuantityDef(200.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap);

        // Initial count should be 2 (two observation entries)
        assertEquals(2, measureObservationDef.getCount(), "Initial count should be 2");
        assertEquals(1, measureObservationDef.getSubjects().size(), "Should have 1 subject");

        // Create exclusion with only one encounter
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, enc1Excl);

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
        var obsMap1 = new ObservationAccumulator(List.of(new ObservationEntry(enc1, new QuantityDef(100.0))));

        // Subject 2: Partial exclusion (2 encounters, 1 excluded)
        Encounter enc2 = createEncounter("encounter-2");
        Encounter enc3 = createEncounter("encounter-3");
        var obsMap2 = new ObservationAccumulator(List.of(
                new ObservationEntry(enc2, new QuantityDef(200.0)),
                new ObservationEntry(enc3, new QuantityDef(300.0))));

        // Subject 3: No exclusions (1 encounter)
        Encounter enc4 = createEncounter("encounter-4");
        var obsMap3 = new ObservationAccumulator(List.of(new ObservationEntry(enc4, new QuantityDef(400.0))));

        measureObservationDef = createMeasureObservationDef();
        measureObservationDef.addResource(SUBJECT_ID_1, obsMap1);
        measureObservationDef.addResource(SUBJECT_ID_2, obsMap2);
        measureObservationDef.addResource(SUBJECT_ID_3, obsMap3);

        // Initial: 1 + 2 + 1 = 4 total observations, 3 subjects
        assertEquals(4, measureObservationDef.getCount(), "Initial count should be 4");
        assertEquals(3, measureObservationDef.getSubjects().size(), "Should have 3 subjects");

        // Create exclusions for subjects 1 and 2
        measurePopulationExclusionDef = createMeasurePopulationExclusionDef();

        // Subject 1: Exclude all (enc1)
        measurePopulationExclusionDef.addResource(SUBJECT_ID_1, createEncounter("encounter-1"));

        // Subject 2: Exclude one (enc2)
        measurePopulationExclusionDef.addResource(SUBJECT_ID_2, createEncounter("encounter-2"));

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
