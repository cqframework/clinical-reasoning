package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

/**
 * Verifies multi-measure / multi-library isolation when one library has unresolvable dependencies
 * (here: a missing ValueSet) and the others are clean. The failing measure must surface its
 * underlying CqlException as a contained OperationOutcome with status=ERROR; the clean measures
 * must complete normally with COMPLETE status, no contained OperationOutcome, and their
 * function-based stratifier strata populated.
 */
@SuppressWarnings("squid:S2699")
class MultiMeasureIsolationTest {

    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MeasureStratifierTest");

    /**
     * Three measures share an evaluation:
     * - A (clean): MultiMeasureCleanA, encounter basis, function-component stratifier.
     * - B (broken): MultiMeasureBrokenLibraryB — IP references a ValueSet not present in the IG;
     *   component stratifier expression is a CQL function taking an Encounter.
     * - C (clean): MultiMeasureCleanC, identical shape to A.
     *
     * <p>Asserts:
     * - A's MeasureReport is COMPLETE with no contained OperationOutcome and a populated stratifier.
     * - B's MeasureReport is ERROR with a contained OperationOutcome whose diagnostic contains
     *   "Unable to locate ValueSet" — i.e. B's failure must surface the engine-level message,
     *   not the masking "Expression result: Initial Population is missing" diagnostic.
     * - C's MeasureReport is COMPLETE with no contained OperationOutcome and a populated stratifier.
     *
     * <p>This guards both: (1) the diagnostic-shape fix for function-based stratifiers, and (2) the
     * multi-measure isolation property — a failure in one library must not poison sibling reports
     * or short-circuit their evaluation.
     */
    @Test
    void oneFailingLibraryDoesNotPoisonSiblings() {
        var when = GIVEN_REPO
                .when()
                .measureId("MultiMeasureCleanA")
                .measureId("MultiMeasureBrokenLibraryB")
                .measureId("MultiMeasureCleanC")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate();

        var then = when.then().hasMeasureReportCount(3);

        // Measure A: clean — must complete without errors and populate stratifier strata.
        then.measureReport("http://example.com/Measure/MultiMeasureCleanA")
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
                .hasContainedResourceCount(0)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .up()
                .firstStratifier();

        // Measure B: broken — must surface the underlying CqlException via contained OperationOutcome.
        then.measureReport("http://example.com/Measure/MultiMeasureBrokenLibraryB")
                .hasMeasureReportStatus(MeasureReportStatus.ERROR)
                .hasContainedOperationOutcome()
                .hasContainedOperationOutcomeMsg("Unable to locate ValueSet");

        // Measure C: clean — must complete without errors and populate stratifier strata,
        // proving the loop did not short-circuit on Measure B.
        then.measureReport("http://example.com/Measure/MultiMeasureCleanC")
                .hasMeasureReportStatus(MeasureReportStatus.COMPLETE)
                .hasContainedResourceCount(0)
                .firstGroup()
                .population(MeasurePopulationType.INITIALPOPULATION)
                .up()
                .firstStratifier();
    }
}
