package org.opencds.cqf.cql.evaluator.measure.common;

public interface MeasureReportScorer<MeasureReportT> {
    public void score(MeasureScoring measureScoring, MeasureReportT measureReport);
}
