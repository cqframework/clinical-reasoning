package org.opencds.cqf.cql.evaluator.measure.r4;

import java.io.IOException;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

public class BloodPressureMeasureProcessorTest {

  protected static Given given =
      Measure.given().repositoryFor("ControllingBloodPressureFHIR");


  @Test(enabled = false, description = "source bundle is missing needed data")
  public void exm165_singlePatient_numerator() throws IOException {

    given.when()
        .measureId("ControllingBloodPressureFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/numer-EXM165")
        .reportType("patient")
        .evaluate()
        .then()
        .firstGroup()
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(1);
  }

  @Test(enabled = false, description = "source bundle is missing needed data")
  public void exm165_population() throws IOException {

    given.when()
        .measureId("ControllingBloodPressureFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .reportType("population")
        .evaluate()
        .then()
        .firstGroup()
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(2);
  }
}
