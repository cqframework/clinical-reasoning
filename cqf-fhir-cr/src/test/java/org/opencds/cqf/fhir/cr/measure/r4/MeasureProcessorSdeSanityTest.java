package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class MeasureProcessorSdeSanityTest {

    protected static Given given = Measure.given().repositoryFor("DM1Measure");

    @Test
    public void measure_eval_unique_extension_list() {
        given.when()
                .measureId("DM1Measure")
                .periodStart("2020-01-01")
                .periodEnd("2022-06-29")
                .subject("Patient/DM1-patient-1")
                .reportType("subject")
                .evaluate()
                .then()
                .hasEvaluatedResourceCount(10)
                .evaluatedResource("Patient/DM1-patient-1")
                .hasPopulations("initial-population")
                .up()
                .evaluatedResource("Observation/DM1-patient-1-observation-1")
                .hasPopulations("numerator");
    }

    @Test
    public void measure_eval_without_measure_period() {
        var report = given.when()
                .measureId("DM1Measure")
                .subject("Patient/DM1-patient-1")
                .reportType("subject")
                .evaluate()
                .then()
                .report();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2019-01-01", formatter.format(report.getPeriod().getStart()));
        assertEquals("2019-12-31", formatter.format(report.getPeriod().getEnd()));
    }
}
