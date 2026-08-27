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
 * StratifierRowKey compositeKey =
 *         StratifierRowKey.withInput("Patient/123", StratifierRowValue.ofResourceId("Encounter/456"));
 * </pre>
 *
 * <h3>Key Components</h3>
 *
 * <ul>
 *   <li>{@code subjectQualified}: The qualified subject reference (e.g., "Patient/123")</li>
 *   <li>{@code inputParam}: Optional input-parameter for function-based stratifiers, or for the
 *       synthetic per-element rows produced by iterable expansion. See {@link StratifierRowValue}.</li>
 * </ul>
 *
 * <h3>Intersection and Alignment</h3>
 *
 * <p>For NON_SUBJECT_VALUE stratifiers, the {@code inputParam} serves two purposes:
 * <ol>
 *   <li><b>Cross-component alignment</b>: Multiple stratifier components (e.g., age + status)
 *       are aligned by matching on the same input parameter</li>
 *   <li><b>Population intersection</b>: Resource-style input params are intersected with
 *       population results to determine which items belong to each stratum. Scalar-style
 *       input params (from iterable expansion of non-resource values) are not intersectable
 *       and instead fall back to subject-level resource attribution.</li>
 * </ol>
 *
 * @see MeasureMultiSubjectEvaluator
 * @see StratifierRowValue
 */
public record StratifierRowKey(String subjectQualified, Optional<StratifierRowValue> inputParam) {

    /**
     * Compact constructor with null validation.
     */
    public StratifierRowKey {
        Objects.requireNonNull(subjectQualified, "subjectQualified must not be null");
        Objects.requireNonNull(inputParam, "inputParam must not be null");
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
     * @param inputParam the input parameter (see {@link StratifierRowValue})
     * @return a composite row key
     */
    public static StratifierRowKey withInput(String subjectQualified, StratifierRowValue inputParam) {
        Objects.requireNonNull(inputParam, "inputParam must not be null");
        return new StratifierRowKey(subjectQualified, Optional.of(inputParam));
    }

    /**
     * Returns just the subject portion of this key.
     * Useful for extracting distinct subjects from a list of row keys.
     */
    public String subjectOnlyKey() {
        return subjectQualified;
    }
}
