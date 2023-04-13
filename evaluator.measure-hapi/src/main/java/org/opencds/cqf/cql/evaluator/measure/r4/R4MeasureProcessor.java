package org.opencds.cqf.cql.evaluator.measure.r4;


import java.util.List;
import java.util.Objects;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.runtime.DateTime;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.cql.evaluator.library.Contexts;
import org.opencds.cqf.cql.evaluator.library.VersionedIdentifiers;
import org.opencds.cqf.cql.evaluator.measure.MeasureEvaluationOptions;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType;
import org.opencds.cqf.cql.evaluator.measure.common.MeasureReportType;
import org.opencds.cqf.cql.evaluator.measure.helper.DateHelper;
import org.opencds.cqf.fhir.api.Repository;

@Named
public class R4MeasureProcessor {
  private final Repository repository;
  private final MeasureEvaluationOptions measureEvaluationOptions;

  public R4MeasureProcessor(Repository repository,
      MeasureEvaluationOptions measureEvaluationOptions) {
    this.repository = Objects.requireNonNull(repository);
    this.measureEvaluationOptions = measureEvaluationOptions != null ? measureEvaluationOptions
        : MeasureEvaluationOptions.defaultOptions();

  }

  public MeasureReport evaluateMeasure(IdType measureId, String periodStart, String periodEnd,
      String reportType, List<String> subjectIds) {
    var measure = this.repository.read(Measure.class, measureId);
    return this.evaluateMeasure(measure, periodStart, periodEnd, reportType, subjectIds);
  }

  // NOTE: Do not make a top-level function that takes a Measure resource. This ensures that
  // the repositories are set up correctly.
  protected MeasureReport evaluateMeasure(Measure measure, String periodStart, String periodEnd,
      String reportType, List<String> subjectIds) {

    if (!measure.hasLibrary()) {
      throw new IllegalArgumentException(
          String.format("Measure %s does not have a primary library specified", measure.getUrl()));
    }

    Interval measurementPeriod = null;
    if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
      measurementPeriod = this.buildMeasurementPeriod(periodStart, periodEnd);
    }

    var id = VersionedIdentifiers.forUrl(measure.getLibrary().get(0).asStringValue());
    var context = Contexts.forRepositoryAndSettings(
        this.measureEvaluationOptions.getEvaluationSettings(), this.repository, id);

    R4MeasureEvaluation measureEvaluator = new R4MeasureEvaluation(context, measure);
    return measureEvaluator.evaluate(MeasureEvalType.fromCode(reportType), subjectIds,
        measurementPeriod);
  }

  protected MeasureReportType evalTypeToReportType(MeasureEvalType measureEvalType) {
    switch (measureEvalType) {
      case PATIENT:
      case SUBJECT:
        return MeasureReportType.INDIVIDUAL;
      case PATIENTLIST:
      case SUBJECTLIST:
        return MeasureReportType.PATIENTLIST;
      case POPULATION:
        return MeasureReportType.SUMMARY;
      default:
        throw new IllegalArgumentException(
            String.format("Unsupported MeasureEvalType: %s", measureEvalType.toCode()));
    }
  }

  private Interval buildMeasurementPeriod(String periodStart, String periodEnd) {
    // resolve the measurement period
    return new Interval(DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodStart, true)),
        true, DateTime.fromJavaDate(DateHelper.resolveRequestDate(periodEnd, false)), true);
  }
}
