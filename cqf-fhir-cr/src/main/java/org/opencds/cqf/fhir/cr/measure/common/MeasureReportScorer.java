package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Map;

public interface MeasureReportScorer<MeasureReportT> {
    public void score(Map<GroupDef, MeasureScoring> measureScoring, MeasureReportT measureReport);
}
