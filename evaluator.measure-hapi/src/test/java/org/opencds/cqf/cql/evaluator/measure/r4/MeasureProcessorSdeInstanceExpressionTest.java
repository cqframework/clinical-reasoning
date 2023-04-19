package org.opencds.cqf.cql.evaluator.measure.r4;

import org.opencds.cqf.cql.evaluator.measure.common.MeasureConstants;
import org.opencds.cqf.cql.evaluator.measure.r4.Measure.Given;
import org.testng.annotations.Test;

public class MeasureProcessorSdeInstanceExpressionTest {

  protected static Given given =
      Measure.given().repositoryFor("ConditionCategoryPoc");

  @Test
  // TODO: The is supposed to fail because some resource is missing, but it's failing for a
  // different reason so we need to debug that
  public void measure_eval_missing_resource() {
    given.when()
        .measureId("ConditionCategoryPOC")
        .periodStart("2022-01-01")
        .periodEnd("2022-12-31")
        .subject("Patient/hist-open-HCC189")
        .reportType("subject")
        .evaluate()
        .then()
        .hasContainedResourceCount(1);
  }

  @Test
  public void measure_eval_non_retrieve_resource() {
    given.when()
        .measureId("ConditionCategoryPOC")
        .periodStart("2022-01-01")
        .periodEnd("2022-12-31")
        .subject("Patient/hist-closed-HCC189")
        .reportType("subject")
        .evaluate()
        .then()
        .hasContainedResource(
            x -> x.getId().startsWith("hist-closed-HCC189-suspecting-algorithm-encounter"))
        .hasExtension(MeasureConstants.EXT_SDE_REFERENCE_URL, 8);
  }
}
