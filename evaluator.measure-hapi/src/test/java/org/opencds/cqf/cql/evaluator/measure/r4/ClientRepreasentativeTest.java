package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;
import java.text.SimpleDateFormat;

import static org.testng.Assert.assertEquals;

public class ClientRepreasentativeTest {
  protected static Given given =
      Measure.given().repositoryFor("DM1Measure");

  @Test
  public void measure_eval_without_measure_period() {
    var report = given.when()
        .measureId("DM1Measure")
        .subject("Patient/DM1-patient-1")
        .reportType("subject")
        .evaluate()
        .then().report();

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    assertEquals(formatter.format(report.getPeriod().getStart()), "2019-01-01");
    assertEquals(formatter.format(report.getPeriod().getEnd()), "2019-12-31");
  }
}
