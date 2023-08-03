package org.opencds.cqf.cql.evaluator.measure.r4;

import static org.testng.Assert.assertEquals;

import java.text.SimpleDateFormat;

import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

import ca.uhn.fhir.context.FhirContext;

public class MeasureProcessorEvaluateTest {

  private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");


  protected static Given given =
      Measure.given().repositoryFor("CaseRepresentation101");


  @Test
  public void measure_eval() {
    var report = given.when()
        .measureId("GlycemicControlHypoglycemicInitialPopulation")
        .periodStart("2022-01-01")
        .periodEnd("2022-06-29")
        .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
        .reportType("subject")
        .evaluate()
        .then().report();

    assertEquals(formatter.format(report.getPeriod().getStart()), "2022-01-01");
    assertEquals(formatter.format(report.getPeriod().getEnd()), "2022-06-29");
  }


  // @Test
  public void measure_eval_with_additional_data() {
    Bundle additionalData = (Bundle) FhirContext.forR4Cached().newJsonParser()
        .parseResource(MeasureProcessorEvaluateTest.class
            .getResourceAsStream(
                "CaseRepresentation101/generated.json"));
    var report = given.when()
        .measureId("GlycemicControlHypoglycemicInitialPopulation")
        .periodStart("2022-01-01")
        .periodEnd("2022-01-31")
        .subject("Patient/980babd9-4979-4b76-978c-946719022dbb")
        .additionalData(additionalData)
        .evaluate()
        .then().report();

    assertEquals(formatter.format(report.getPeriod().getStart()), "2022-01-01");
    assertEquals(formatter.format(report.getPeriod().getEnd()), "2022-06-29");
  }
}
