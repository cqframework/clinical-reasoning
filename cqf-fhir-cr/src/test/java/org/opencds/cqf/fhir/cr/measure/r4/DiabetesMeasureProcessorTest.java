package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

class DiabetesMeasureProcessorTest {

    protected static Given given = Measure.given().repositoryFor("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR");

    @Test
    void a1c_singlePatient_numerator() {
        given.when()
                .measureId("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
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
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
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
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
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
