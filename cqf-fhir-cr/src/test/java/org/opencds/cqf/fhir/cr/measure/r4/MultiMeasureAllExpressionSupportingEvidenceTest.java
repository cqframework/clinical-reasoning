package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import java.util.List;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceMode;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;

/**
 * Verifies {@link SupportingEvidenceMode} over the multi-measure path, where several measures share
 * one evaluation and one CQL engine.
 *
 * <p>The single-measure tests already reach the same per-measure plumbing with a one-element
 * measure list, so what is specific here is breadth rather than a separate code path: multiple
 * measures plus subject mode is the only combination routed to
 * {@code R4MultiMeasureService.subjectReport}, and the only case where
 * {@code CompositeEvaluationResultsPerMeasure} carries more than one measure's results. A report
 * picking up a sibling measure's expressions can therefore only surface here.
 */
@SuppressWarnings({"java:S2699"})
class MultiMeasureAllExpressionSupportingEvidenceTest {

    private static final String COHORT_URL = "http://example.com/Measure/CohortBooleanSupportingEvidence";
    private static final String UNDECLARED_URL = "http://example.com/Measure/SupportingEvidenceUndeclared";

    private static Given givenMode(SupportingEvidenceMode mode) {
        return MultiMeasure.given().repositoryFor("MeasureTest").supportingEvidenceMode(mode);
    }

    private static List<Extension> reportLevelEvidence(MeasureReport report) {
        return report.getExtension().stream()
                .filter(e -> EXT_SUPPORTING_EVIDENCE_URL.equals(e.getUrl()))
                .toList();
    }

    private static List<String> evidenceNames(MeasureReport report) {
        return reportLevelEvidence(report).stream()
                .map(e -> e.getExtensionByUrl("name").getValue().primitiveValue())
                .toList();
    }

    /**
     * Both measures are evaluated in a single call under ALL_EXPRESSIONS. Each report must carry
     * its own expressions and none of its sibling's, which is what distinguishes a per-measure
     * evidence collection from one accumulated across the batch.
     */
    @Test
    void allExpressionsReachesEveryMeasureInTheBatch() {
        var then = givenMode(SupportingEvidenceMode.ALL_EXPRESSIONS)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .measureId("SupportingEvidenceUndeclared")
                .reportType("subject")
                .subject("patient-9")
                .evaluate()
                .then();

        // One subject, so one bundle carrying one report per measure.
        then.hasBundleCount(1).hasMeasureReportCount(2);

        var cohortNames = evidenceNames(then.measureReport(COHORT_URL).measureReport());
        var undeclaredNames = evidenceNames(then.measureReport(UNDECLARED_URL).measureReport());

        assertFalse(cohortNames.isEmpty(), "cohort measure should carry report-level evidence");
        assertFalse(undeclaredNames.isEmpty(), "undeclared measure should carry report-level evidence");

        assertTrue(cohortNames.contains("test tuple"), cohortNames::toString);
        assertTrue(cohortNames.contains("always true"), cohortNames::toString);

        assertTrue(undeclaredNames.contains("Time Value"), undeclaredNames::toString);
        assertTrue(undeclaredNames.contains("Quantity Value"), undeclaredNames::toString);

        // Neither report may pick up the other's expressions.
        assertFalse(cohortNames.contains("Time Value"), cohortNames::toString);
        assertFalse(cohortNames.contains("Quantity Value"), cohortNames::toString);
        assertFalse(undeclaredNames.contains("test tuple"), undeclaredNames::toString);
        assertFalse(undeclaredNames.contains("always true"), undeclaredNames::toString);
    }

    /**
     * The collector gates on {@code mode != ALL_EXPRESSIONS || !isSubjectScoped(evalType)}. The
     * subject-mode tests cover the first half of that guard; this covers the second, on the
     * multi-measure population branch, where report-level evidence stays off whatever the mode.
     */
    @Test
    void allExpressionsAddsNoReportLevelEvidenceToPopulationReports() {
        var then = givenMode(SupportingEvidenceMode.ALL_EXPRESSIONS)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .measureId("SupportingEvidenceUndeclared")
                .reportType("population")
                .evaluate()
                .then();

        // Population mode yields one bundle holding a report per measure.
        then.hasBundleCount(1).hasMeasureReportCount(2);

        assertEquals(
                List.of(),
                evidenceNames(then.measureReport(COHORT_URL).measureReport()),
                "population reports are not subject-scoped, so they carry no report-level evidence");
        assertEquals(
                List.of(),
                evidenceNames(then.measureReport(UNDECLARED_URL).measureReport()),
                "population reports are not subject-scoped, so they carry no report-level evidence");
    }

    /**
     * The default mode must not start emitting report-level evidence just because the evaluation
     * runs through the multi-measure path.
     */
    @Test
    void declaredModeAddsNoReportLevelEvidenceInTheBatch() {
        var then = givenMode(SupportingEvidenceMode.DECLARED)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .measureId("SupportingEvidenceUndeclared")
                .reportType("subject")
                .subject("patient-9")
                .evaluate()
                .then();

        assertEquals(
                List.of(),
                evidenceNames(then.measureReport(COHORT_URL).measureReport()),
                "DECLARED must leave the cohort report free of report-level evidence");
        assertEquals(
                List.of(),
                evidenceNames(then.measureReport(UNDECLARED_URL).measureReport()),
                "DECLARED must leave the undeclared report free of report-level evidence");
    }
}
