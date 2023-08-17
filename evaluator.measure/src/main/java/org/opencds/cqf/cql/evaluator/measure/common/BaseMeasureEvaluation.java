package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;
import java.util.Objects;

import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.runtime.Interval;

public abstract class BaseMeasureEvaluation<MeasureT, MeasureReportT, SubjectT> {

  protected MeasureDefBuilder<MeasureT> measureDefBuilder;
  protected MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder;
  protected CqlEngine context;
  protected MeasureT measure;
  protected String measurementPeriodParameterName;

  protected BaseMeasureEvaluation(CqlEngine context, MeasureT measure,
      MeasureDefBuilder<MeasureT> measureDefBuilder,
      MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder) {
    this(context, measure, measureDefBuilder, measureReportBuilder,
        MeasureConstants.MEASUREMENT_PERIOD_PARAMETER_NAME);
  }

  protected BaseMeasureEvaluation(CqlEngine context, MeasureT measure,
      MeasureDefBuilder<MeasureT> measureDefBuilder,
      MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> measureReportBuilder,
      String measurementPeriodParameterName) {
    this.context = Objects.requireNonNull(context, "context is a required argument");
    this.measure = Objects.requireNonNull(measure, "measure is a required argument");
    this.measureDefBuilder =
        Objects.requireNonNull(measureDefBuilder, "measureDefBuilder is a required argument");
    this.measureReportBuilder =
        Objects.requireNonNull(measureReportBuilder, "measureReportBuilder is a required argument");
    this.measurementPeriodParameterName = Objects.requireNonNull(measurementPeriodParameterName,
        "measurementPeriodParameterName is a required argument");
  }

  public MeasureReportT evaluate(MeasureEvalType measureEvalType, List<String> subjectIds) {
    return this.evaluate(measureEvalType, subjectIds, null);
  }

  public MeasureReportT evaluate(MeasureEvalType measureEvalType, List<String> subjectIds,
      Interval measurementPeriod) {
    Objects.requireNonNull(subjectIds, "subjectIds is a required parameter");
    Objects.requireNonNull(measureEvalType, "measureEvalType is a required parameter");

    MeasureDef measureDef = this.measureDefBuilder.build(measure);
    MeasureEvaluator measureEvaluation =
        new MeasureEvaluator(context, this.measurementPeriodParameterName);
    measureDef =
        measureEvaluation.evaluate(measureDef, measureEvalType, subjectIds, measurementPeriod);

    // TODO: This is a bit hokey. Need to figure out a better way get/set the period.
    var actualPeriod =
        (Interval) context.getState().getParameters().get(this.measurementPeriodParameterName);

    return this.measureReportBuilder.build(measure, measureDef,
        this.evalTypeToReportType(measureEvalType), actualPeriod, subjectIds);
  }

  protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
    switch (measureEvalType) {
      case PATIENT:
      case SUBJECT:
        return MeasureReportType.INDIVIDUAL;
      case PATIENTLIST:
        return MeasureReportType.PATIENTLIST;
      case SUBJECTLIST:
        return MeasureReportType.SUBJECTLIST;
      case POPULATION:
        return MeasureReportType.SUMMARY;
      default:
        throw new IllegalArgumentException(
            String.format("Unsupported MeasureEvalType: %s", measureEvalType.toCode()));
    }
  }
}
