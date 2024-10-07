package org.opencds.cqf.fhir.cr.measure.common;

public interface MeasureReportScorer<MeasureReportT> {
    public void score(MeasureDef measureDef, MeasureReportT measureReport);
}
