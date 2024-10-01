package org.opencds.cqf.fhir.cr.measure.r4;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class BloodPressureMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("ControllingBloodPressureFHIR");

    @Test
    @Disabled("source bundle is missing needed data")
    void exm165_singlePatient_numerator() throws IOException {

        given.when()
                .measureId("ControllingBloodPressureFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM165")
                .reportType("patient")
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1);
    }

    @Test
    @Disabled("source bundle is missing needed data")
    void exm165_population() throws IOException {

        given.when()
                .measureId("ControllingBloodPressureFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay().atZone(ZoneId.systemDefault()))
                .reportType("population")
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(2);
    }
}
