package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * Thrown when pre-evaluation validation of a Measure fails with blocking errors.
 * Contains the structured {@link ValidationResult} so callers can inspect individual issues.
 */
public class MeasureValidationException extends RuntimeException {

    private final ValidationResult validationResult;

    public MeasureValidationException(String measureUrl, ValidationResult validationResult) {
        super(formatMessage(measureUrl, validationResult));
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    private static String formatMessage(String measureUrl, ValidationResult validationResult) {
        List<ValidationIssue> errors = validationResult.getBlockingErrors();
        var errorMessages = errors.stream()
                .map(issue -> "[%s] %s".formatted(issue.code(), issue.description()))
                .toList();
        return "Measure validation failed for '%s' with %d error(s):\n%s"
                .formatted(measureUrl, errors.size(), String.join("\n", errorMessages));
    }
}
