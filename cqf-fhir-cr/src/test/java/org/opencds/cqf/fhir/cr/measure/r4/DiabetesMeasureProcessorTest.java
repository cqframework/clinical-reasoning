package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class DiabetesMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR");

    @Test
    void a1c_singlePatient_numerator() {
        given.when()
                .measureId("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JUNE, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-CMS122-Patient")
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
    void a1c_population() throws IOException {
        given.when()
                .measureId("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JUNE, 1).atStartOfDay(ZoneId.systemDefault()))
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

    @Test
    void a1c_additionalData() {

        Bundle additionalData = (Bundle) FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(DiabetesMeasureProcessorTest.class.getResourceAsStream(
                        "DiabetesHemoglobinA1cHbA1cPoorControl9FHIR/CMS122-AdditionalData-bundle.json"));
        given.when()
                .measureId("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR")
                .periodStart(LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2020, Month.JUNE, 1).atStartOfDay(ZoneId.systemDefault()))
                .subject("Patient/numer-CMS122-Patient")
                .reportType("patient")
                .additionalData(additionalData)
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1);
    }
}
