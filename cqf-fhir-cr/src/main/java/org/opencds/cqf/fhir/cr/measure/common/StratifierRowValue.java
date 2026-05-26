package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * The input-parameter slot of a {@link StratifierRowKey}.
 *
 * <p>NON_SUBJECT_VALUE stratifiers fan a single subject out into multiple rows. Two flavours
 * exist:
 *
 * <ul>
 *   <li><b>Resource-style</b> ({@link Resource}): the row's inputParam is a function input
 *       (FHIR resource ID or stringified primitive). These keys intersect with the population's
 *       real resource keys, so {@link #isIntersectable()} returns {@code true}.</li>
 *   <li><b>Scalar-style</b> ({@link Scalar}): the row was synthesised by iterable expansion
 *       for a non-resource element ({@code value_<i>_<v>}) or a {@code null} element
 *       ({@code null_<i>}). These keys exist only to give each list element a unique row and
 *       must NOT be intersected against population resources, so {@link #isIntersectable()}
 *       returns {@code false}.</li>
 * </ul>
 *
 * <p>Factories cover the call sites that previously hand-built strings:
 *
 * <ul>
 *   <li>{@link #ofFunctionInput(Object)} — function-input objects (FHIR resource or primitive).
 *       Replaces the old {@code normalizeResourceKey(Object)} helper.</li>
 *   <li>{@link #ofIterableElement(Object, int)} — single element of an iterable result.
 *       Replaces the old {@code normalizeValueKey(Object, int)} helper.</li>
 *   <li>{@link #ofResourceId(String)} — for already-normalised IDs (tests, internal use).</li>
 * </ul>
 */
public sealed interface StratifierRowValue permits StratifierRowValue.Resource, StratifierRowValue.Scalar {

    /**
     * Stable string form of this value, used as the unique inputParam slot in
     * {@link StratifierRowKey} and as the comparable form in {@link SubjectResourceKey}.
     */
    String legacyString();

    /**
     * {@code true} if this value can be intersected against population resource keys; only
     * {@link Resource} returns true. Iterable-derived {@link Scalar} keys are synthetic
     * uniquifiers and must fall back to subject-level resource lookup instead.
     */
    boolean isIntersectable();

    /**
     * For a function input — either a FHIR resource (uses its versionless ID) or any other
     * object (falls back to {@link String#valueOf(Object)}, preserving the prior behaviour of
     * the now-removed {@code normalizeResourceKey} helper).
     */
    static StratifierRowValue ofFunctionInput(Object obj) {
        if (obj instanceof IBaseResource resource
                && resource.getIdElement() != null
                && !resource.getIdElement().isEmpty()) {
            return new Resource(resource.getIdElement().toVersionless().getValue());
        }
        return new Resource(String.valueOf(obj));
    }

    /**
     * For a single element of an iterable stratifier result. Resource elements collapse to a
     * {@link Resource} key (still intersectable). {@code null} and non-resource scalar
     * elements produce a {@link Scalar} that uses {@code index} for uniqueness within the
     * iterable.
     */
    static StratifierRowValue ofIterableElement(@Nullable Object value, int index) {
        if (value == null) {
            return new Scalar(index, null);
        }
        if (value instanceof IBaseResource resource
                && resource.getIdElement() != null
                && !resource.getIdElement().isEmpty()) {
            return new Resource(resource.getIdElement().toVersionless().getValue());
        }
        return new Scalar(index, value);
    }

    /**
     * For a pre-normalised resource-ID string (test/internal use). Use
     * {@link #ofFunctionInput(Object)} when you have the original object.
     */
    static StratifierRowValue ofResourceId(String resourceId) {
        return new Resource(Objects.requireNonNull(resourceId, "resourceId must not be null"));
    }

    /**
     * Intersectable inputParam carrying a resource ID or stringified primitive.
     */
    record Resource(String resourceId) implements StratifierRowValue {

        public Resource {
            Objects.requireNonNull(resourceId, "resourceId must not be null");
        }

        @Override
        public String legacyString() {
            return resourceId;
        }

        @Override
        public boolean isIntersectable() {
            return true;
        }
    }

    /**
     * Non-intersectable inputParam produced by iterable expansion. Encodes
     * {@code value_<index>_<value>} for non-null elements and {@code null_<index>} for
     * {@code null} elements.
     */
    record Scalar(int index, @Nullable Object value) implements StratifierRowValue {

        private static final String VALUE_PREFIX = "value_";
        private static final String NULL_PREFIX = "null_";

        @Override
        public String legacyString() {
            return value == null ? NULL_PREFIX + index : VALUE_PREFIX + index + "_" + value;
        }

        @Override
        public boolean isIntersectable() {
            return false;
        }
    }
}
