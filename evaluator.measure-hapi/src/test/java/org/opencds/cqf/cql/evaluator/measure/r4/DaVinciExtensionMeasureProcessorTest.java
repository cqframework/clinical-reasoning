package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;


public class DaVinciExtensionMeasureProcessorTest {

  protected static Given given = Measure.given().repositoryFor("r4/BreastCancerScreeningFHIR");


  @Test
  public void exm125_numerator() {
    given.when()
        .measureId("BreastCancerScreeningFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2019-12-31")
        .subject("Patient/numer-EXM125")
        .reportType("subject")
        .evaluate().then()
        .firstGroup().hasScore("1.0")
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(1).up().up()
        .evaluatedResource("Patient/numer-EXM125")
        .hasPopulations("initial-population", "denominator-exclusion").up()
        .evaluatedResource("DiagnosticReport/numer-EXM125-3").hasPopulations("numerator");
  }
}
