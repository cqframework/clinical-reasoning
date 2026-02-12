package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * A typed key representing a subject + resource value pair for population counting.
 *
 * <p>This record solves the problem of counting population members correctly when the same
 * resource value can appear for multiple subjects. For example, with a date-basis measure,
 * Patient/A and Patient/B might both have the date "2024-01-01" in their initial population.
 * Using just the date string would deduplicate these, but using SubjectResourceKey preserves
 * both occurrences.
 *
 * <h3>When to use SubjectResourceKey vs just resourceValue</h3>
 *
 * <ul>
 *   <li><b>FHIR Resources</b> (Encounter, Procedure, etc.): Resource IDs are globally unique,
 *       so {@code resourceValue} alone is sufficient. Use {@link #resourceOnly(String)}.</li>
 *   <li><b>Primitive types</b> (Date, Integer, etc.): The same value can belong to multiple
 *       subjects, so both subject and value are needed. Use {@link #of(String, String)}.</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 *
 * <pre>
 * // For FHIR resources (globally unique IDs)
 * SubjectResourceKey key1 = SubjectResourceKey.resourceOnly("Encounter/123");
 *
 * // For primitive types (need subject context)
 * SubjectResourceKey key2 = SubjectResourceKey.of("Patient/A", "2024-01-01");
 * SubjectResourceKey key3 = SubjectResourceKey.of("Patient/B", "2024-01-01");
 * // key2 and key3 are NOT equal, preserving both patients' dates
 * </pre>
 *
 * @param subjectId the qualified subject ID (e.g., "Patient/123"), or null for resource-only keys
 * @param resourceValue the resource value as a string (resource ID or primitive value)
 */
public record SubjectResourceKey(@Nullable String subjectId, String resourceValue) {

    /**
     * Compact constructor with validation.
     */
    public SubjectResourceKey {
        Objects.requireNonNull(resourceValue, "resourceValue must not be null");
    }

    /**
     * Creates a key for FHIR resources where the resource ID is globally unique.
     *
     * <p>Use this when the population basis is a FHIR resource type (Encounter, Procedure, etc.)
     * where resource IDs are unique across subjects.
     *
     * @param resourceValue the resource ID (e.g., "Encounter/123")
     * @return a key with only the resource value (no subject context)
     */
    public static SubjectResourceKey resourceOnly(String resourceValue) {
        return new SubjectResourceKey(null, resourceValue);
    }

    /**
     * Creates a key for primitive types where the same value can appear for multiple subjects.
     *
     * <p>Use this when the population basis is a primitive type (Date, Integer, etc.)
     * where the same value can legitimately belong to multiple subjects.
     *
     * @param subjectId the qualified subject ID (e.g., "Patient/123")
     * @param resourceValue the primitive value as a string (e.g., "2024-01-01")
     * @return a key with both subject and value context
     */
    public static SubjectResourceKey of(String subjectId, String resourceValue) {
        Objects.requireNonNull(subjectId, "subjectId must not be null for subject-qualified keys");
        return new SubjectResourceKey(subjectId, resourceValue);
    }

    /**
     * Creates a key from a StratifierRowKey.
     *
     * <p>For NON_SUBJECT_VALUE stratifiers with primitive basis, this preserves the
     * subject context needed for correct counting.
     *
     * @param rowKey the stratifier row key
     * @param isPrimitiveBasis true if the population basis is a primitive type
     * @return a SubjectResourceKey with appropriate subject context
     */
    public static SubjectResourceKey fromRowKey(StratifierRowKey rowKey, boolean isPrimitiveBasis) {
        String inputParam = rowKey.inputParamId().orElseThrow(
                () -> new IllegalArgumentException("RowKey must have an inputParamId"));

        if (isPrimitiveBasis) {
            return of(rowKey.subjectQualified(), inputParam);
        } else {
            return resourceOnly(inputParam);
        }
    }

    /**
     * Returns true if this key includes subject context.
     */
    public boolean hasSubject() {
        return subjectId != null;
    }
}
