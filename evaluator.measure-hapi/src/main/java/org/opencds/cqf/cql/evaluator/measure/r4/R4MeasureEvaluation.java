package org.opencds.cqf.cql.evaluator.measure.r4;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.evaluator.measure.common.BaseMeasureEvaluation;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR R4 structures.
 */
public class R4MeasureEvaluation
    extends BaseMeasureEvaluation<Measure, MeasureReport, DomainResource> {

  public R4MeasureEvaluation(Context context, Measure measure) {
    super(context, measure, new R4MeasureDefBuilder(), new R4MeasureReportBuilder());
  }
}
