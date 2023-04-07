package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.testng.annotations.Test;


@Test
public class DaVinciExtensionMeasureProcessorTest extends BaseMeasureProcessorTestB {

  public DaVinciExtensionMeasureProcessorTest() {
    super("BreastCancerScreeningFHIR");
  }

  @Test
  public void exm125_numerator() {
    // Given

    // When

    // Then
    Measure.Assert.that("BreastCancerScreeningFHIR", "2019-01-01", "2019-12-31")
        .repository(this.repository).subject("Patient/numer-EXM125")
        .reportType("subject").evaluate()
        .firstGroup().hasScore("1.0")
        .population("numerator").hasCount(1).up()
        .population("denominator").hasCount(1).up().up()
        .passes(this::exm125_numerator_validation);
  }

  protected void exm125_numerator_validation(MeasureReport report) {
    MeasureValidationUtils.validateEvaluatedResourceExtension(report.getEvaluatedResource(),
        "Patient/numer-EXM125", "initial-population", "denominator-exclusion");
    MeasureValidationUtils.validateEvaluatedResourceExtension(report.getEvaluatedResource(),
        "DiagnosticReport/numer-EXM125-3", "numerator");
  }
}
