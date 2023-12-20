package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.MeasureReport;
import org.junit.jupiter.api.Test;

public class Dstu3MeasureProcessorTest {
    @Test
    public void exm105_fullSubjectId() {
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
    public void exm105_fullSubjectId_invalidMeasureScorer() {
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
        MeasureReport report = null;
        try {
            report = when.then().report();
        } catch (RuntimeException e) {
            assertTrue(e.getCause().toString().contains(errorMsg));
        }
        assertNull(report);
    }
}
