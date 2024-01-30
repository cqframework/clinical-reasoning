package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Dstu3MeasureProcessorTest {
    @Test
    void exm105_fullSubjectId() {
        Measure.given()
                .repositoryFor("EXM105FHIR3Measure")
                .when()
                .measureId("measure-EXM105-FHIR3-8.0.000")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/denom-EXM105-FHIR3")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(1);
    }

    @Test
    void exm105_fullSubjectId_invalidMeasureScorer() {
        // Removed MeasureScorer from Measure, should trigger exception
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("measure-EXM105-FHIR3-8.0.000")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/denom-EXM105-FHIR3")
                .reportType("subject")
                .evaluate();

        String errorMsg = "MeasureScoring must be specified on Measure";
        var e = assertThrows(IllegalArgumentException.class, () -> when.then());
        assertEquals(errorMsg, e.getMessage());
    }

    @Test
    void evaluateThrowsErrorWithEmptyMeasure() {
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("Empty")
                .evaluate();
        var e = assertThrows(IllegalArgumentException.class, () -> when.then());
        assertTrue(e.getMessage().contains("does not have a primary library"));
    }

    @Test
    // Ensures an error is thrown when we can't find the library
    void evaluateThrowsErrorWhenLibraryUnavailable() {
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("LibraryMissingContent")
                .evaluate();
        var e = assertThrows(IllegalStateException.class, () -> when.then());
        assertTrue(e.getMessage().contains("Unable to load CQL/ELM"));
    }

    @Test
    void evaluateThrowsErrorWhenLibraryIsMissingContent() {
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("LibraryMissingContent")
                .evaluate();
        var e = assertThrows(IllegalStateException.class, () -> when.then());
        assertTrue(e.getMessage().contains("Unable to load CQL/ELM for library"));
    }

    @Test
    void evaluateSucceedsWithMinimalMeasure() {
        var when = Measure.given()
                .repositoryFor("MinimalMeasure")
                .when()
                .measureId("Minimal")
                .evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(0, report.getGroup().size());
    }
}
