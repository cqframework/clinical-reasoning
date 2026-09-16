package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;

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
public sealed interface StratifierRowValue
        permits StratifierRowValue.Resource, StratifierRowValue.ResourceElement, StratifierRowValue.Scalar {

    /**
     * Stable string form of this value, used as the unique inputParam slot in
     * {@link StratifierRowKey} and as the comparable form in {@link SubjectResourceKey}.
     */
    String legacyString();

    /**
     * The value used to intersect this row against population resource keys. For most values this is
     * the same as {@link #legacyString()}. It differs for {@link ResourceElement}, whose
     * {@code legacyString()} carries a per-element discriminator (so multiple values for one input
     * land in distinct strata) but whose intersection value is the underlying input id (so the
     * input is still counted correctly in each stratum it fans into).
     */
    default String intersectionValue() {
        return legacyString();
    }

    /**
     * {@code true} if this value can be intersected against population resource keys; {@link Resource}
     * and {@link ResourceElement} return true. Iterable-derived {@link Scalar} keys are synthetic
     * uniquifiers and must fall back to subject-level resource lookup instead.
     */
    boolean isIntersectable();

    /**
     * For a function input — either a FHIR resource (uses its versionless ID) or any other
     * object (falls back to {@link String#valueOf(Object)}, preserving the prior behaviour of
     * the now-removed {@code normalizeResourceKey} helper).
     */
    static StratifierRowValue ofFunctionInput(Object obj) {
        return new Resource(functionInputId(obj));
    }

    /**
     * The intersectable id for a function input — a FHIR resource's versionless {@code Type/id}, or
     * {@link String#valueOf(Object)} for a primitive input (matching {@link #ofFunctionInput(Object)}).
     */
    private static String functionInputId(Object obj) {
        final String resourceId = resourceIdOrNull(obj);
        return resourceId != null ? resourceId : String.valueOf(obj);
    }

    /**
     * For one element of a multi-valued function output produced by a single input. The row must be
     * distinct per element (so each value forms its own stratum), yet still intersect the population
     * on the underlying input id (so that input is counted in each stratum it fans into). Uniqueness
     * comes from {@code index}; {@link #intersectionValue()} returns the input id.
     */
    static StratifierRowValue ofFunctionInputElement(Object input, int index) {
        return new ResourceElement(functionInputId(input), index);
    }

    /**
     * Returns the versionless {@code Type/id} string for a FHIR-resource input — whether it arrives
     * as a HAPI {@link IBaseResource} or as a CQL-5 engine-native {@link ClassInstance} FHIR
     * resource — or {@code null} for anything else (primitives, resources without an id).
     * Mirrors {@code MeasureMultiSubjectEvaluator.normalizePopulationKey} so a function input and
     * the population resource it should intersect with produce the same key.
     */
    @Nullable
    private static String resourceIdOrNull(@Nullable Object obj) {
        if (obj instanceof IBaseResource resource
                && resource.getIdElement() != null
                && !resource.getIdElement().isEmpty()) {
            return resource.getIdElement().toVersionless().getValue();
        }
        if (obj instanceof ClassInstance classInstance) {
            return ClassInstanceHelper.getId(classInstance);
        }
        return null;
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
        final String resourceId = resourceIdOrNull(value);
        if (resourceId != null) {
            return new Resource(resourceId);
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
     * Intersectable inputParam for one element of a multi-valued function output. {@code resourceId}
     * is the underlying input id (used for population intersection); {@code index} makes the key
     * unique per element so each value forms its own stratum. Encodes {@code <resourceId>#e<index>}.
     */
    record ResourceElement(String resourceId, int index) implements StratifierRowValue {

        public ResourceElement {
            Objects.requireNonNull(resourceId, "resourceId must not be null");
        }

        @Override
        public String legacyString() {
            return resourceId + "#e" + index;
        }

        @Override
        public String intersectionValue() {
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
