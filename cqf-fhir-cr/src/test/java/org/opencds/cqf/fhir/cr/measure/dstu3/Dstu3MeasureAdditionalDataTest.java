package org.opencds.cqf.fhir.cr.measure.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.dstu3.Measure.Given;

class Dstu3MeasureAdditionalDataTest {

    protected static Given given = Measure.given().repositoryFor("EXM105FHIR3MeasurePartBundle");

    // LUKETODO: POPULATION: populationBasis: [Encounter], result class: List: Encounter
    @Test
    void measureAdditionalData() {

        var parser = FhirContext.forDstu3Cached().newJsonParser();
        var additionalData = (Bundle) parser.parseResource(Dstu3MeasureAdditionalDataTest.class.getResourceAsStream(
                "EXM105FHIR3MeasurePartBundle/EXM105FHIR3MeasureAdditionalBundle.json"));

        given.when()
                .measureId("measure-EXM105-FHIR3-8.0.000")
                .periodStart("2019-01-01")
                .periodEnd("2020-01-01")
                .subject("Patient/denom-EXM105-FHIR3")
                .reportType("subject")
                .additionalData(additionalData)
                .evaluate()
                .then()
                .firstGroup()
                .population("numerator")
                .hasCount(0)
                .up()
                .population("denominator")
                .hasCount(1);
    }
}
