package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

@SuppressWarnings("squid:S2699")
class DiabetesMeasureProcessorTest {

    // LUKETODO:  should we fix the measures or the tests?
    // this looks like it could be production-like data so I'm not sure, and can't tell from the
    // commit history
    // The current error is that the new code thinks these are criteria-based stratifiers, and they're
    // now failing validation

    protected static Given given = Measure.given().repositoryFor("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR");

    @Test
    void a1c_singlePatient_numerator() {
        given.when()
                .measureId("DiabetesHemoglobinA1cHbA1cPoorControl9FHIR")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/numer-CMS122-Patient")
                .reportType("subject")
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
    void a1c_population() {
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
                .reportType("subject")
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
