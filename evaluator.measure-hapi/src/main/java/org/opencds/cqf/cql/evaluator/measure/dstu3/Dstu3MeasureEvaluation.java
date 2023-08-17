package org.opencds.cqf.cql.evaluator.measure.dstu3;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.evaluator.measure.common.BaseMeasureEvaluation;

/**
 * Implementation of MeasureEvaluation on top of HAPI FHIR DSTU3 structures.
 */
public class Dstu3MeasureEvaluation
    extends BaseMeasureEvaluation<Measure, MeasureReport, DomainResource> {

  public Dstu3MeasureEvaluation(CqlEngine context, Measure measure) {
    super(context, measure, new Dstu3MeasureDefBuilder(), new Dstu3MeasureReportBuilder());
  }

}
