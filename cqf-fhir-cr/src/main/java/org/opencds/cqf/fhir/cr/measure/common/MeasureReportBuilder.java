package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import org.opencds.cqf.cql.engine.runtime.Interval;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

public interface MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> {
    MeasureReportT build(
            MeasureT measure,
            MeasureReportDef def,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds);
}
