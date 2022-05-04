package org.opencds.cqf.cql.evaluator.measure.common;

public interface MeasureReportAggregator<MeasureReportT> {

    /**
     * This functions takes a set of MeasureReports that were calculated independently and aggregates them into one.
     * 
     * @param reports
     * @return the aggregate MeasureReport
     */
    MeasureReportT aggregate(Iterable<MeasureReportT> reports);
    
}
