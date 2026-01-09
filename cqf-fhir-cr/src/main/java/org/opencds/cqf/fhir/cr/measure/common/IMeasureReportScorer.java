package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Interface for scoring FHIR MeasureReports.
 *
 * <p><strong>DEPRECATION NOTICE:</strong> This interface is deprecated and will be removed in a future release.
 * For internal use, this interface will be replaced by {@link MeasureReportDefScorer}
 * integrated into the evaluation workflow in Part 2.
 * See: integrate-measure-def-scorer-part2-integration PRP
 */
@Deprecated
public interface IMeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);
}
