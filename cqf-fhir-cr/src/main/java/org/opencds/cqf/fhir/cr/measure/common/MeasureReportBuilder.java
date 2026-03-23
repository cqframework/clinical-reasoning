package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;
import org.opencds.cqf.cql.engine.runtime.Interval;

public interface MeasureReportBuilder<MeasureReportT> {
    MeasureReportT build(
            MeasureDef def,
            MeasureEvaluationState state,
            MeasureReportType measureReportType,
            Interval measurementPeriod,
            List<String> subjectIds);
}
