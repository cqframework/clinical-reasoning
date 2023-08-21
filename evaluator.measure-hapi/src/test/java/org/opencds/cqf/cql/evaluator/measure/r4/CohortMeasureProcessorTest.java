package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.junit.jupiter.api.Test;


public class CohortMeasureProcessorTest {

  protected static Given given =
      Measure.given().repositoryFor("SeenPatients");

  @Test
  public void seenPatients_singlePatient() {
    given.when()
        .measureId("SeenPatients")
        .periodStart("2019-01-01")
        .periodEnd("2019-12-31")
        .subject("Patient/ip-SeenPatients")
        .reportType("subject")
        .evaluate()
        .then()
        .firstGroup()
        .population("initial-population").hasCount(1);
  }
}
