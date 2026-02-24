package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Objects;
import java.util.Optional;

/**
 * Typed key representing a single stratifier "row" for measure evaluation.
 *
 * <p>A stratifier row key uniquely identifies a unit of stratification:
 * <ul>
 *   <li><b>Subject-basis stratifiers</b>: One row per subject (e.g., Patient/123)</li>
 *   <li><b>NON_SUBJECT_VALUE stratifiers</b>: One row per subject + input parameter combination
 *       (e.g., Patient/123 with Encounter/456 as the function input)</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 *
 * <pre>
 * // Subject-basis stratifier (e.g., gender stratification)
 * StratifierRowKey subjectKey = StratifierRowKey.subjectOnly("Patient/123");
 *
 * // NON_SUBJECT_VALUE stratifier (e.g., encounter status function)
 * StratifierRowKey compositeKey = StratifierRowKey.withInput("Patient/123", "Encounter/456");
 * </pre>
 *
 * <h3>Key Components</h3>
 *
 * <ul>
 *   <li>{@code subjectQualified}: The qualified subject reference (e.g., "Patient/123")</li>
 *   <li>{@code inputParamId}: Optional input parameter ID for function-based stratifiers
 *       (e.g., "Encounter/456" when stratifying by a function that takes Encounter as input)</li>
 * </ul>
 *
 * <h3>Intersection and Alignment</h3>
 *
 * <p>For NON_SUBJECT_VALUE stratifiers, the {@code inputParamId} serves two purposes:
 * <ol>
 *   <li><b>Cross-component alignment</b>: Multiple stratifier components (e.g., age + status)
 *       are aligned by matching on the same input parameter</li>
 *   <li><b>Population intersection</b>: The input parameter is intersected with population
 *       results to determine which items belong to each stratum</li>
 * </ol>
 *
 * @see MeasureMultiSubjectEvaluator
 */
public record StratifierRowKey(String subjectQualified, Optional<String> inputParamId) {

    /**
     * Compact constructor with null validation.
     */
    public StratifierRowKey {
        Objects.requireNonNull(subjectQualified, "subjectQualified must not be null");
        Objects.requireNonNull(inputParamId, "inputParamId must not be null");
    }

    /**
     * Creates a row key for subject-basis stratifiers (no input parameter).
     *
     * @param subjectQualified the qualified subject reference (e.g., "Patient/123")
     * @return a row key with no input parameter
     */
    public static StratifierRowKey subjectOnly(String subjectQualified) {
        return new StratifierRowKey(subjectQualified, Optional.empty());
    }

    /**
     * Creates a row key for NON_SUBJECT_VALUE stratifiers with an input parameter.
     *
     * @param subjectQualified the qualified subject reference (e.g., "Patient/123")
     * @param inputParamId the input parameter ID (e.g., "Encounter/456")
     * @return a composite row key
     */
    public static StratifierRowKey withInput(String subjectQualified, String inputParamId) {
        Objects.requireNonNull(inputParamId, "inputParamId must not be null");
        return new StratifierRowKey(subjectQualified, Optional.of(inputParamId));
    }

    /**
     * Returns true if this row key has an input parameter (i.e., is for a NON_SUBJECT_VALUE stratifier).
     */
    public boolean hasInputParam() {
        return inputParamId.isPresent();
    }

    /**
     * Returns just the subject portion of this key.
     * Useful for extracting distinct subjects from a list of row keys.
     */
    public String subjectOnlyKey() {
        return subjectQualified;
    }

    /**
     * Returns the input parameter ID, or null if not present.
     * Convenience method for code that needs nullable semantics.
     */
    public String inputParamIdOrNull() {
        return inputParamId.orElse(null);
    }

    /**
     * Parses a legacy string format row key into a StratifierRowKey.
     *
     * <p>Legacy format uses "|" as delimiter:
     * <ul>
     *   <li>"Patient/123" → subjectOnly("Patient/123")</li>
     *   <li>"Patient/123|Encounter/456" → withInput("Patient/123", "Encounter/456")</li>
     * </ul>
     *
     * @param legacyKey the legacy string format key
     * @return the parsed StratifierRowKey
     */
    public static StratifierRowKey fromLegacyString(String legacyKey) {
        Objects.requireNonNull(legacyKey, "legacyKey must not be null");
        int idx = legacyKey.indexOf('|');
        if (idx < 0) {
            return subjectOnly(legacyKey);
        }
        String subject = legacyKey.substring(0, idx);
        String input = legacyKey.substring(idx + 1);
        return withInput(subject, input);
    }

    /**
     * Converts this key to the legacy string format for compatibility.
     *
     * <p>Prefer using StratifierRowKey directly when possible.
     * This method is provided for backward compatibility with code
     * that still expects String-based row keys.
     *
     * @return the legacy string format ("Patient/123" or "Patient/123|Encounter/456")
     */
    public String toLegacyString() {
        return inputParamId.map(id -> subjectQualified + "|" + id).orElse(subjectQualified);
    }
}
