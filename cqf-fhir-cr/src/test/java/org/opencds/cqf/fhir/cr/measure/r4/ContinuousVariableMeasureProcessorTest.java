package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class ContinuousVariableMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("CMS111");

    @Test
    void cms111_singlePatient() {
        given.when()
                .measureId("CMS111")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/measure-strat1-EXM111")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }
}
