package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;

import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.junit.jupiter.api.Test;

public class MeasureProcessorEvaluatedResourceTest {

  protected static Given given =
      Measure.given().repositoryFor("ContentBundleUpdated");

  @Test
  public void measure_eval_contained_is_unique() {
    var report = given.when()
        .measureId("HTN1Measure")
        .periodStart("2020-08-16")
        .periodEnd("2022-08-16")
        .subject("Patient/HTN1-patient-1")
        .reportType("subject")
        .evaluate()
        .then()
        .hasEvaluatedResourceCount(9)
        .hasContainedResourceCount(4)
        .hasMeasureVersion("1.0.1")
        .evaluatedResource("Encounter/HTN1-patient-1-encounter-2")
        .hasPopulations("initial-population").up()
        .evaluatedResource("Observation/HTN1-patient-1-observation-3")
        .hasPopulations("numerator").up()
        .firstGroup()
        .population("initial-population").hasCount(1).up().up()
        .report();

    // check contained duplicates
    HashSet<String> containedIdSet = new HashSet<>();
    report.getContained().forEach(x -> containedIdSet.add(x.getId()));
    assertEquals(report.getContained().size(), containedIdSet.size());
  }
}
