package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        var period = report.getPeriod();
        var start = period.getStart();
        var end = period.getEnd();

        // The CQL engine sets times with an unspecified offset to the _current_ system offset,
        // using the rules for the system default timezone. A given timezone may have variable
        // offsets from UTC (e.g. daylight savings time), and the current offset may be different
        // than the expected offset for a given date.
        var now = OffsetDateTime.now();

        assertEquals("2019-01-01", formatter.format(start.toInstant().atOffset(now.getOffset())));
        assertEquals("2019-12-31", formatter.format(end.toInstant().atOffset(now.getOffset())));
    }
}
