package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;

import org.opencds.cqf.cql.engine.runtime.Interval;

public interface MeasureReportBuilder<MeasureT, MeasureReportT, SubjectT> {
    MeasureReportT build(MeasureT measure, MeasureDef def, MeasureReportType measureReportType, Interval measurementPeriod, List<String> subjectIds);
}
