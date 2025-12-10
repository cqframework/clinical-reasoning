package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Interface for version-specific MeasureReport scoring.
 *
 * <p><strong>Note:</strong> This interface will be removed in a future PR.
 * For external consumers (e.g., cdr-cr project), use
 * {@link MeasureReportScoringFhirAdapter#score(org.hl7.fhir.instance.model.api.IBaseResource, org.hl7.fhir.instance.model.api.IBaseResource)}
 * for version-agnostic post-hoc scoring.
 * For internal use, this interface will be replaced by {@link MeasureDefScorer}
 * integrated into the evaluation workflow in Part 2.
 */
public interface IMeasureReportScorer<MeasureReportT> {
    void score(String measureUrl, MeasureDef measureDef, MeasureReportT measureReport);
}
