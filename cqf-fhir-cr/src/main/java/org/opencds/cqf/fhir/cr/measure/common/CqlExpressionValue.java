package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;

/**
 * Wrapper around the raw {@code Object} returned by
 * {@link ExpressionResult#getValue()}.
 * <p>
 * Centralizes the null / Boolean / Iterable / scalar normalization that was previously
 * scattered across measure-evaluation call sites, so the contract between the CQL engine
 * and the measure pipeline is testable in one place. The internal {@code raw} field is
 * intentionally typed as {@code Object} so a future migration to the upstream sealed
 * {@code Value} type touches only this class, not its callers.
 */
public final class CqlExpressionValue {

    private static final CqlExpressionValue EMPTY = new CqlExpressionValue(null, Collections.emptySet());

    private final @Nullable Object raw;
    private final Set<Object> evaluatedResources;

    private CqlExpressionValue(@Nullable Object raw, Set<Object> evaluatedResources) {
        this.raw = raw;
        this.evaluatedResources = evaluatedResources;
    }

    /**
     * Wraps an {@link ExpressionResult}. Accepts a null result and yields an empty wrapper.
     */
    public static CqlExpressionValue of(@Nullable ExpressionResult result) {
        if (result == null) {
            return EMPTY;
        }
        Set<Object> resources = result.getEvaluatedResources();
        return new CqlExpressionValue(result.getValue(), resources != null ? resources : Collections.emptySet());
    }

    /**
     * Wraps a raw value plus its evaluated-resource set directly. Useful for tests and
     * for callers that already hold the underlying value.
     */
    public static CqlExpressionValue ofRaw(@Nullable Object value, @Nullable Set<Object> evaluatedResources) {
        return new CqlExpressionValue(value, evaluatedResources != null ? evaluatedResources : Collections.emptySet());
    }

    /**
     * Returns a wrapper whose value is null and whose evaluated-resources set is empty.
     */
    public static CqlExpressionValue empty() {
        return EMPTY;
    }

    public boolean isNull() {
        return raw == null;
    }

    public boolean isBoolean() {
        return raw instanceof Boolean;
    }

    public boolean isTrue() {
        return Boolean.TRUE.equals(raw);
    }

    public boolean isIterable() {
        return raw instanceof Iterable<?>;
    }

    public boolean isMap() {
        return raw instanceof Map<?, ?>;
    }

    /**
     * True when the underlying value is null, an empty {@link Iterable} (or {@link Collection}),
     * or an empty {@link Map}.
     */
    public boolean isEmpty() {
        if (raw == null) {
            return true;
        }
        if (raw instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (raw instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (raw instanceof Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }
        return false;
    }

    public Optional<Boolean> asBoolean() {
        return raw instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Returns the underlying value as a typed {@link Map} when it is one, otherwise empty.
     * The single unchecked cast is localized here so call sites do not have to repeat it.
     * Used for measure-observation accumulators where the CQL engine produces
     * {@code Map<inputResource, outputValue>}.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<Object, Object>> asMap() {
        return raw instanceof Map<?, ?> map ? Optional.of((Map<Object, Object>) map) : Optional.empty();
    }

    /**
     * Normalizes the value to an {@link Iterable}: null becomes an empty list, an existing
     * iterable is returned as-is, and a scalar is wrapped in a single-element list.
     */
    @SuppressWarnings("unchecked")
    public Iterable<Object> asIterable() {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof Iterable<?>) {
            return (Iterable<Object>) raw;
        }
        return Collections.singletonList(raw);
    }

    /**
     * Like {@link #asIterable()} but preserves a true-null result rather than coercing to
     * an empty list. Mirrors the legacy {@code evaluateSupportingCriteria} contract where
     * a null indicates "no result evaluated" and is meaningful to downstream consumers.
     */
    @SuppressWarnings("unchecked")
    public @Nullable Iterable<Object> asIterableOrNull() {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Iterable<?>) {
            return (Iterable<Object>) raw;
        }
        return Collections.singletonList(raw);
    }

    /**
     * Resolves a population-criterion value to an iterable of population members.
     * <p>
     * <ul>
     *   <li>If the value is null, returns an empty list.</li>
     *   <li>If the value is {@link Boolean#TRUE}, looks up {@code subjectType} in the
     *       provided {@link EvaluationResult} and returns its single resolved value
     *       (the subject context resource).</li>
     *   <li>If the value is {@link Boolean#FALSE}, returns an empty list.</li>
     *   <li>Otherwise, normalizes via {@link #asIterable()}.</li>
     * </ul>
     * Throws {@link CqlExpressionValueException} when the {@code subjectType} lookup
     * yields no expression result for a {@code Boolean.TRUE} criterion.
     */
    public Iterable<Object> resolveForPopulation(String subjectType, EvaluationResult evaluationResult) {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof Boolean b) {
            if (!b) {
                return Collections.emptyList();
            }
            ExpressionResult subjectResult = evaluationResult.get(subjectType);
            if (subjectResult == null) {
                throw new CqlExpressionValueException(
                        "expression result is null for subject type: %s".formatted(subjectType));
            }
            return Collections.singletonList(subjectResult.getValue());
        }
        return asIterable();
    }

    /**
     * Returns the underlying value(s) as a {@link Set} that uses FHIR-resource and CQL-type
     * identity semantics: scalars are wrapped in a single-element set, iterables are flattened
     * into the set, and a null value yields an empty set.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Set<Object> valueAsSet() {
        if (raw == null) {
            return new HashSetForFhirResourcesAndCqlTypes<>();
        }
        if (raw instanceof Iterable<?>) {
            return new HashSetForFhirResourcesAndCqlTypes<>((Iterable) raw);
        }
        return new HashSetForFhirResourcesAndCqlTypes<>(raw);
    }

    /**
     * Returns the underlying value(s) as a {@link List} with nulls filtered out. A scalar
     * becomes a single-element list, an iterable is flattened (preserving order, dropping
     * nulls), and a null value yields an empty list.
     */
    public List<Object> nonNullValues() {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .filter(Objects::nonNull)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return List.of(raw);
    }

    public Set<Object> evaluatedResources() {
        return evaluatedResources;
    }

    /**
     * Escape hatch for callers that still need the underlying {@link Object}. Preserved
     * during the migration from the legacy {@code Object}-typed pipeline to the eventual
     * sealed {@code Value} type.
     */
    public @Nullable Object raw() {
        return raw;
    }
}
