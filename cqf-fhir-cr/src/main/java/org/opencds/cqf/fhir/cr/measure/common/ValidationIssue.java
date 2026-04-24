package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;

/**
 * A single validation issue discovered during pre-evaluation Measure validation.
 * Each issue carries a machine-readable {@code code}, a human-readable {@code description},
 * actionable {@code remediation} guidance, and an optional {@code location} within the Measure resource.
 *
 * @param severity    the severity level of this issue
 * @param code        machine-readable error code (e.g. {@code "LIBRARY_NOT_FOUND"})
 * @param description human-readable description of the problem
 * @param remediation actionable guidance on how to resolve the issue
 * @param location    optional path within the Measure resource (e.g. {@code "Measure.library"})
 */
public record ValidationIssue(
        ValidationSeverity severity,
        String code,
        String description,
        String remediation,
        @Nullable String location) {

    public ValidationIssue(ValidationSeverity severity, String code, String description, String remediation) {
        this(severity, code, description, remediation, null);
    }

    public boolean isError() {
        return severity == ValidationSeverity.ERROR;
    }

    public boolean isWarning() {
        return severity == ValidationSeverity.WARNING;
    }
}
