package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class MeasureProcessorSdeSanityTest {

    protected static Given given = Measure.given().repositoryFor("DM1Measure");

    @Test
    void measure_eval_unique_extension_list() {
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
    void measure_eval_without_measure_period() {
        var report = given.when()
                .measureId("DM1Measure")
                .subject("Patient/DM1-patient-1")
                .reportType("subject")
                .evaluate()
                .then()
                .report();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var period = report.getPeriod();
        var start = period.getStart();
        var end = period.getEnd();

        // clinical-reasoning overrides the CQL engine to set times with an unspecified offset to the UTC offset,
        // Otherwise, CQL would set it to the local server's timezone offset.

        assertEquals("2019-01-01", formatter.format(start.toInstant().atOffset(ZoneOffset.UTC)));
        assertEquals("2019-12-31", formatter.format(end.toInstant().atOffset(ZoneOffset.UTC)));
    }
}
