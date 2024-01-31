package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
