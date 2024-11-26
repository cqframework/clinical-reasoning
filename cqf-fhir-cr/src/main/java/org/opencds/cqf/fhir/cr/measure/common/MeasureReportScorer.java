package org.opencds.cqf.fhir.cr.measure.common;

public interface MeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);
}
