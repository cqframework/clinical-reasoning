package org.opencds.cqf.fhir.cr.measure.r4;

import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.junit.jupiter.api.Test;


public class DaVinciExtensionMeasureProcessorTest {

  protected static Given given = Measure.given().repositoryFor("BreastCancerScreeningFHIR");

  @Test
  // TODO: Huh. Why is patient not showing up in the 'initial-population' list?
  public void exm125_numerator() {
    given.when()
        .measureId("BreastCancerScreeningFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2019-12-31")
        .subject("Patient/numer-EXM125")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup().hasScore("1.0")
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(1).up().up()
        .evaluatedResource("Patient/numer-EXM125")
        .hasPopulations("initial-population", "denominator-exclusion").up()
        .evaluatedResource("DiagnosticReport/numer-EXM125-3").hasPopulations("numerator");
  }
}
