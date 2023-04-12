package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

public class EncounterBasisMeasureProcessorTest {
  protected static Given given =
      Measure.given().repositoryFor("r4/DischargedonAntithromboticTherapyFHIR");

  @Test
  public void exm104_singlePatient() {
    given.when()
        .measureId("DischargedonAntithromboticTherapyFHIR")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/numer-EXM104")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup()
        .population("initial-population").hasCount(1);
  }
}
