package org.opencds.cqf.fhir.cr.measure.common;

public interface MeasureReportScorer<MeasureReportT> {
    public void score(MeasureScoring measureScoring, MeasureReportT measureReport);
}
