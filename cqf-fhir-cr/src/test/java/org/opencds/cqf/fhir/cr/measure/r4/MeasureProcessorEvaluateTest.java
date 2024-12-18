package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;

class MeasureProcessorEvaluateTest {

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
}
