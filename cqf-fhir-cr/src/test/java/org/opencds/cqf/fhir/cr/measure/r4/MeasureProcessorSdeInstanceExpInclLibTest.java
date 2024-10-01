package org.opencds.cqf.fhir.cr.measure.r4;

import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

class MeasureProcessorSdeInstanceExpInclLibTest {

    protected static Given given = Measure.given().repositoryFor("DM1Measure");

    @Test
    void measure_eval_non_retrieve_resource_incl_lib_ref() {
        given.when()
                .measureId("DM1Measure")
                .periodStart(LocalDate.of(2020, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/DM1-patient-1")
                .reportType("subject")
                .evaluate()
                .then()
                .hasContainedResourceCount(3);
    }
}
