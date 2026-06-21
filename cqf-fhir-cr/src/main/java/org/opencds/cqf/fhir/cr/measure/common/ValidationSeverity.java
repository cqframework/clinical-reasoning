package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Severity levels for pre-evaluation validation issues found during Measure validation.
 * Maps to FHIR {@code OperationOutcome.IssueSeverity} when surfaced in a MeasureReport.
 */
public enum ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}
