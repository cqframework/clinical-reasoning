package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;

import java.text.SimpleDateFormat;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

public class MeasureProcessorEvaluateTest {


  protected static Given given =
      Measure.given().repositoryFor("CaseRepresentation101");


  @Test
  public void measure_eval_without_measure_period() {
    var report = given.when()
        .measureId("GlycemicControlHypoglycemicInitialPopulation")
        .periodStart("2022-01-01")
        .periodEnd("2022-06-29")
        .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
        .reportType("subject")
        .evaluate()
        .then().report();

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    assertEquals(formatter.format(report.getPeriod().getStart()), "2022-01-01");
    assertEquals(formatter.format(report.getPeriod().getEnd()), "2022-06-29");
  }
}
