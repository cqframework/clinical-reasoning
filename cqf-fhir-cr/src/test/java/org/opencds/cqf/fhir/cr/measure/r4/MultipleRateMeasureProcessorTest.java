package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class MultipleRateMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("FHIR347");

    @Test
    void fhir347_singlePatient() {
        given.when()
                .measureId("FHIR347")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer1-EXM347")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }
}
