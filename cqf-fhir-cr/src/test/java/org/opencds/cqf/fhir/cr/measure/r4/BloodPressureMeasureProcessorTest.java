package org.opencds.cqf.fhir.cr.measure.r4;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class BloodPressureMeasureProcessorTest {

  protected static Given given =
      Measure.given().repositoryFor("ControllingBloodPressureFHIR");

  @Test
  @Disabled("source bundle is missing needed data")
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

  @Test
  @Disabled("source bundle is missing needed data")
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
