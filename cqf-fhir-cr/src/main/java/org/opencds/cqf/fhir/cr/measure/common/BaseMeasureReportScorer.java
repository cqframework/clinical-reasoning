package org.opencds.cqf.fhir.cr.measure.common;

public abstract class BaseMeasureReportScorer<MeasureReportT>
    implements MeasureReportScorer<MeasureReportT> {
  protected Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
    if (numeratorCount == null) {
      numeratorCount = 0;

    }
    if (denominatorCount != null && denominatorCount != 0) {
      return numeratorCount / (double) denominatorCount;
    }

    return null;
  }
}
