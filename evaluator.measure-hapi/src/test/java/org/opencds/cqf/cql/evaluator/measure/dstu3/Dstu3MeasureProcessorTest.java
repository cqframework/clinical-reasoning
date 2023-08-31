package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.opencds.cqf.cql.evaluator.measure.dstu3.Measure.Given;
import org.junit.jupiter.api.Test;

public class Dstu3MeasureProcessorTest {

  protected static Given given =
      Measure.given().repositoryFor("EXM105FHIR3Measure");

  @Test
  public void exm105_fullSubjectId() {
    given.when()
        .measureId("measure-EXM105-FHIR3-8.0.000")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/denom-EXM105-FHIR3")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup().population("numerator").hasCount(0).up()
        .population("denominator").hasCount(1);
  }
}
