package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class MeasureProcessorEvaluateTest {

    protected static Given given = Measure.given().repositoryFor("CaseRepresentation101");

    /**
     * test to validate that measure with MeasureScorer specified at the group level
     * and nothing on measure-level MeasureScorer
     */
    @Test
    void measure_eval_group_measurescorer() {
        var when = Measure.given()
                .repositoryFor("DischargedonAntithromboticTherapyFHIR")
                .when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();
        MeasureReport report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(3, report.getGroupFirstRep().getPopulation().get(0).getCount());
    }

    @Test
    void measure_eval_group_measurescorer_invalidMeasureScore() {
        // Removed MeasureScorer from Measure, should trigger exception
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();

        String errorMsg =
                "MeasureScoring must be specified on Group or Measure for Measure: https://madie.cms.gov/Measure/DischargedonAntithromboticTherapyFHIR";
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertEquals(errorMsg, e.getMessage());
    }
}
