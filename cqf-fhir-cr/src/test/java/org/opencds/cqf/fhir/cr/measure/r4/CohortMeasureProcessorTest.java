package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class CohortMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("SeenPatients");

    @Test
    void seenPatients_singlePatient() {
        given.when()
                .measureId("SeenPatients")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/ip-SeenPatients")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }
}
