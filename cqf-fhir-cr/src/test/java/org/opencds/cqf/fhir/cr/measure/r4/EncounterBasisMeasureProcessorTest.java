package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class EncounterBasisMeasureProcessorTest {
    protected static Given given = Measure.given().repositoryFor("DischargedonAntithromboticTherapyFHIR");

    @Test
    void exm104_singlePatient() {
        given.when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM104")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .population("initial-population")
                .hasCount(1);
    }
}
