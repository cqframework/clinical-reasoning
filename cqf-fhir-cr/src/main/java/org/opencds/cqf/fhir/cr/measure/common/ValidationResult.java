package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Accumulates {@link ValidationIssue} instances produced during pre-evaluation Measure validation.
 * Provides convenience methods to query for blocking errors and warnings.
 */
public class ValidationResult {

    private final List<ValidationIssue> issues = new ArrayList<>();

    public void addIssue(ValidationIssue issue) {
        issues.add(issue);
    }

    public void merge(ValidationResult other) {
        issues.addAll(other.issues);
    }

    public List<ValidationIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(ValidationIssue::isError);
    }

    public boolean hasWarnings() {
        return issues.stream().anyMatch(ValidationIssue::isWarning);
    }

    public List<ValidationIssue> getBlockingErrors() {
        return issues.stream().filter(ValidationIssue::isError).toList();
    }

    public boolean isEmpty() {
        return issues.isEmpty();
    }
}
