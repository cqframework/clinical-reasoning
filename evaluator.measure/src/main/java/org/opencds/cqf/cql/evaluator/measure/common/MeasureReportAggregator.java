package org.opencds.cqf.cql.evaluator.measure.common;

public interface MeasureReportAggregator<MeasureReportT> {
    public MeasureReportT aggregate(Iterable<MeasureReportT> reports);
}