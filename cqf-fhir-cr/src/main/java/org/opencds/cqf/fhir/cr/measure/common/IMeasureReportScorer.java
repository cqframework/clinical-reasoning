package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

public interface IMeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureReportDef measureDef, MeasureReportT measureReport);
}
