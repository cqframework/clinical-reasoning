package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.junit.jupiter.api.Test;

public class ContinuousVariableMeasureProcessorTest {

  protected static Given given = Measure.given().repositoryFor("CMS111");

  @Test
  public void cms111_singlePatient() {
    given.when()
        .measureId("CMS111")
        .periodStart("2019-01-01")
        .periodEnd("2020-01-01")
        .subject("Patient/measure-strat1-EXM111")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup()
        .population("initial-population").hasCount(1);
  }
}
