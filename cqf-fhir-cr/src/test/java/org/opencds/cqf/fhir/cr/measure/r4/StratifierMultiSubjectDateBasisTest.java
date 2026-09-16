package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
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
        var allDatesStratum = "all-dates";

        // 2 patients × 5 dates each = 10 total dates
        // All dates map to the same stratum 'all-dates'
        // The stratum should count all 10, not deduplicate to 5
        GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMeasure")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                // Group-level count: 2 patients × 5 dates = 10
                .hasCount(10)
                .up()
                .firstStratifier()
                .hasCodeText("Constant Stratifier")
                .hasStratumCount(1)
                .stratumByComponentValueText(allDatesStratum)
                .population(MeasurePopulationType.INITIALPOPULATION)
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
        var allDatesStratum = "all-dates";

        GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMeasure")
                .subject("Patient/patient-a")
                .evaluate()
                .then()
                .hasGroupCount(1)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                // Single patient × 5 dates = 5
                .hasCount(5)
                .up()
                .firstStratifier()
                .hasStratumCount(1)
                .stratumByComponentValueText(allDatesStratum)
                .population(MeasurePopulationType.INITIALPOPULATION)
                .hasCount(5)
                .up()
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Reproduces the HEDIS 2025 (NCQA ENP-Reporting) stratifier bug using the REAL
     * {@code Product Line Stratifier} function body (a query ending in {@code return all
     * mmInfo.payer.code} over stubbed member-months), paired with a scalar {@code Age Stratifier}.
     *
     * <p>The stratifier component whose function returns a <b>list</b> is not unwrapped: its stratum
     * value is emitted as the raw CQL representation ({@code 'MMO'}, with quotes) instead of
     * {@code MMO}. With empty/multi-element lists (real member-months data) this degrades further and
     * the stratifier collapses. Expected: three strata for a single member with 12 enrollment dates,
     * Age constant ("18-19"), Product Line MMO x2 / MCD x9 / MEP x1, each with clean component values.
     */
    @Test
    void hedisEnrollmentComponentStratifierRendersCleanCodes() {
        MeasureReport report = GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMultiComponentMeasure")
                .subject("Patient/patient-a")
                .evaluate()
                .then()
                .measureReport();

        var group = report.getGroup().stream()
                .filter(g -> "Enrollment".equals(g.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Enrollment group in report"));
        assertEquals(12, group.getPopulationFirstRep().getCount(), "initial-population count");

        assertFalse(group.getStratifier().isEmpty(), "Enrollment group produced no stratifier");
        var strata = group.getStratifierFirstRep().getStratum();
        assertEquals(3, strata.size(), "expected three strata (MMO, MCD, MEP)");

        // Map each stratum's ProductLine component value -> its initial-population count.
        Map<String, Integer> productLineCounts = new HashMap<>();
        for (var stratum : strata) {
            assertEquals(2, stratum.getComponent().size(), "each stratum should carry Age + ProductLine");
            var productLine = stratum.getComponent().stream()
                    .filter(c -> c.getCode().hasText()
                            && "ProductLine".equals(c.getCode().getText()))
                    .map(c -> c.getValue().getText())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("stratum is missing a ProductLine component"));

            // The defect: the list-valued "Product Line Stratifier" (`return all mmInfo.payer.code`)
            // rendered its element as the raw CQL representation ('MMO') instead of the bare code MMO.
            // The engine still emits three strata; the malformed values are what break the report
            // downstream. This assertion fails clearly (not with an NPE) when the fix is absent.
            assertNotNull(productLine, "ProductLine component value.text was null");
            assertFalse(
                    productLine.contains("'"),
                    "ProductLine value should be a bare code, but got the raw CQL representation: " + productLine);
            productLineCounts.put(productLine, stratum.getPopulationFirstRep().getCount());
        }

        assertEquals(
                Map.of("MMO", 2, "MCD", 9, "MEP", 1),
                productLineCounts,
                "expected clean product-line strata with correct counts");
    }

    /**
     * A stratifier <em>function</em> that returns a multi-element list for a single input must fan out
     * into one stratum per value (the input counting toward each), not collapse into a single stratum
     * with a comma-joined value.
     *
     * <p>Here {@code @2026-01-12} is enrolled in two product lines (MEP and MMO) in the same month, so
     * {@code Multi Payer Product Line Stratifier} returns {@code {MEP, MMO}} for that date. Expected:
     * that date counts toward BOTH the MEP and MMO strata. Per-input counts: MMO x3 (01-01, 01-02,
     * 01-12) / MCD x9 / MEP x1 (01-12) — distinct from a subject-level count, which would report 12 for
     * every stratum. No component value is comma-joined.
     */
    @Test
    void hedisEnrollmentMultiValueProductLineFansOutPerValue() {
        MeasureReport report = GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisMultiValueComponentMeasure")
                .subject("Patient/patient-a")
                .evaluate()
                .then()
                .measureReport();

        var group = report.getGroup().stream()
                .filter(g -> "Enrollment".equals(g.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Enrollment group in report"));
        assertEquals(12, group.getPopulationFirstRep().getCount(), "initial-population count");

        assertFalse(group.getStratifier().isEmpty(), "Enrollment group produced no stratifier");
        var strata = group.getStratifierFirstRep().getStratum();
        assertEquals(3, strata.size(), "the multi-value date must fan out, yielding three strata (MMO, MCD, MEP)");

        Map<String, Integer> productLineCounts = new HashMap<>();
        for (var stratum : strata) {
            assertEquals(2, stratum.getComponent().size(), "each stratum should carry Age + ProductLine");
            var productLine = stratum.getComponent().stream()
                    .filter(c -> c.getCode().hasText()
                            && "ProductLine".equals(c.getCode().getText()))
                    .map(c -> c.getValue().getText())
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("stratum is missing a ProductLine component"));
            assertFalse(
                    productLine.contains(","),
                    "ProductLine value should fan out into separate strata, not comma-join: " + productLine);
            productLineCounts.put(productLine, stratum.getPopulationFirstRep().getCount());
        }

        assertEquals(
                Map.of("MMO", 3, "MCD", 9, "MEP", 1),
                productLineCounts,
                "the multi-payer date (01-12) must count toward both MMO and MEP, per input");
    }

    /**
     * Reproduces the empty/collapsed stratifier reported for HEDIS 2025 (NCQA ENP-Reporting):
     * {@code "stratifier": [ { "id": "enrollment-stratifier" } ]} with no {@code stratum} array.
     *
     * <p>Both stratifier components ({@code Null Age Subject}, {@code Null Product Line Subject})
     * evaluate to null for the member. Before the fix, a null-valued scalar component was silently
     * dropped in {@code MeasureEvaluator.handleNonBooleanBasisComponent}, so when every component was
     * null no results were recorded and {@code buildSubjectResultsTable} produced zero strata — the
     * stratifier collapsed entirely. After the fix a null component records an explicit null result,
     * so the member lands in a single "null" stratum (matching how a null-returning function component
     * already behaves) instead of vanishing.
     */
    @Test
    void hedisEnrollmentNullComponentsProduceNullStratumNotEmpty() {
        MeasureReport report = GIVEN.when()
                .measureId("StratifierMultiSubjectDateBasisNullComponentMeasure")
                .subject("Patient/patient-a")
                .evaluate()
                .then()
                .measureReport();

        var group = report.getGroup().stream()
                .filter(g -> "Enrollment".equals(g.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Enrollment group in report"));
        assertEquals(12, group.getPopulationFirstRep().getCount(), "initial-population count");

        assertFalse(group.getStratifier().isEmpty(), "Enrollment group produced no stratifier");
        var strata = group.getStratifierFirstRep().getStratum();

        // The defect collapsed this to zero strata. A subject whose components are all null must
        // still be stratified — into a single "null" stratum carrying every declared component.
        assertEquals(
                1, strata.size(), "null-valued components should yield one 'null' stratum, not an empty stratifier");
        var stratum = strata.get(0);
        assertEquals(2, stratum.getComponent().size(), "stratum should carry Age + ProductLine components");
        for (var component : stratum.getComponent()) {
            assertNotNull(component.getValue(), "component value should be present");
            assertEquals(
                    "null", component.getValue().getText(), "null component should render the 'null' sentinel value");
        }
        assertEquals(
                12, stratum.getPopulationFirstRep().getCount(), "all 12 enrollment dates fall in the null stratum");
    }
}
