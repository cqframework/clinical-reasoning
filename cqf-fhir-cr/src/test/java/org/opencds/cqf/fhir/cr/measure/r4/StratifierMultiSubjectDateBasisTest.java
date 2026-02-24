package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Integration test for stratifier population counting with multi-subject date-basis measures.
 *
 * <p>This test validates the fix for the stratifier deduplication bug where primitive population
 * values (like Date) were incorrectly deduplicated across subjects. The bug caused stratum counts
 * to be lower than expected when multiple patients had the same date values in their populations.
 *
 * <h3>Test Scenario</h3>
 * <ul>
 *   <li>Measure has date-basis population (populationBasis: date)</li>
 *   <li>Initial Population CQL returns 5 dates per patient: { @2024-01-01, ..., @2024-01-05 }</li>
 *   <li>Stratifier is a function that returns 'all-dates' for every date</li>
 *   <li>With 2 patients, the stratum should have count = 10 (5 dates × 2 patients)</li>
 * </ul>
 *
 * <h3>Bug Behavior (before fix)</h3>
 * The stratum count was 5 because the code deduplicated by date value across subjects,
 * counting only the 5 unique dates instead of all 10 date occurrences.
 *
 * <h3>Expected Behavior (after fix)</h3>
 * The stratum count should be 10, preserving all date occurrences per subject.
 *
 * @see org.opencds.cqf.fhir.cr.measure.common.SubjectResourceKey
 */
@SuppressWarnings("squid:S2699")
class StratifierMultiSubjectDateBasisTest {

    private static final Given GIVEN = Measure.given().repositoryFor("StratifierMultiSubjectDateBasis");

    /**
     * Validates that stratifier counts preserve subject-specific results for date-basis measures.
     *
     * <p>With 2 patients each returning 5 dates, and a stratifier that maps all dates to the
     * same stratum value ('all-dates'), the stratum initial-population count should be 10.
     *
     * <p>This test would fail with the deduplication bug, showing count = 5 instead of 10.
     */
    @Test
    void stratifierPreservesMultiSubjectDateCounts() {
        var allDatesStratum = new CodeableConcept().setText("all-dates");

        // 2 patients × 5 dates each = 10 total dates
        // All dates map to the same stratum 'all-dates'
        // The stratum should count all 10, not deduplicate to 5
        GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMeasure")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .population("initial-population")
                // Group-level count: 2 patients × 5 dates = 10
                .hasCount(10)
                .up()
                .firstStratifier()
                .hasCodeText("Constant Stratifier")
                .hasStratumCount(1)
                .stratum(allDatesStratum)
                .population("initial-population")
                // Stratum count should also be 10 (all dates map to this stratum)
                // BUG: Before fix, this was 5 due to deduplication
                .hasCount(10)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Validates that a single subject correctly counts dates in the stratum.
     *
     * <p>With 1 patient returning 5 dates, the stratum should have count = 5.
     * This ensures the fix doesn't break single-subject behavior.
     */
    @Test
    void stratifierCountsCorrectlyForSingleSubject() {
        var allDatesStratum = new CodeableConcept().setText("all-dates");

        GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMeasure")
                .subject("Patient/patient-a")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .population("initial-population")
                // Single patient × 5 dates = 5
                .hasCount(5)
                .up()
                .firstStratifier()
                .hasStratumCount(1)
                .stratum(allDatesStratum)
                .population("initial-population")
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }
}
