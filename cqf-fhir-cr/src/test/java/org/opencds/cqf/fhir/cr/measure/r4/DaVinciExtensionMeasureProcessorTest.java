package org.opencds.cqf.fhir.cr.measure.r4;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class DaVinciExtensionMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("BreastCancerScreeningFHIR");

    // TODO: Huh. Why is patient not showing up in the 'initial-population' list?
    @Test
    void exm125_numerator() {
        given.when()
                .measureId("BreastCancerScreeningFHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2019, Month.DECEMBER, 31).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-EXM125")
                .reportType("subject")
                .evaluate()
                .then()
                .firstGroup()
                .hasScore("1.0")
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .up()
                .up()
                .evaluatedResource("Patient/numer-EXM125")
                .hasPopulations("initial-population", "denominator-exclusion")
                .up()
                .evaluatedResource("DiagnosticReport/numer-EXM125-3")
                .hasPopulations("numerator");
    }
}
