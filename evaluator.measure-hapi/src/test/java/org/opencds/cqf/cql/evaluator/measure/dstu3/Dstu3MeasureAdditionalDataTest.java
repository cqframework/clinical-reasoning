package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.hl7.fhir.dstu3.model.Bundle;
import org.opencds.cqf.cql.evaluator.measure.dstu3.Measure.Given;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class Dstu3MeasureAdditionalDataTest {

  protected static Given given =
      Measure.given().repositoryFor("EXM105FHIR3MeasurePartBundle");

  @Test
  public void testMeasureAdditionalData() {

    var parser = FhirContext.forDstu3Cached().newJsonParser();
    var additionalData = (Bundle) parser.parseResource(Dstu3MeasureAdditionalDataTest.class
        .getResourceAsStream(
            "EXM105FHIR3MeasurePartBundle/EXM105FHIR3MeasureAdditionalBundle.json"));

    given.when()
        .measureId("EXM105-FHIR3-8.0.000")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/denom-EXM105-FHIR3")
        .reportType("subject")
        .additionalData(additionalData)
        .evaluate()
        .then()
        .firstGroup().population("numerator").hasCount(0).up()
        .population("denominator").hasCount(1);
  }
}
