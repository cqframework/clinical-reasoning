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
import org.opencds.cqf.cql.engine.runtime.Value;

/**
 * Wrapper around the raw {@code Value} returned by
 * {@link ExpressionResult#getValue()}.
 * <p>
 * Centralizes the null / Boolean / Iterable / scalar normalization that was previously
 * scattered across measure-evaluation call sites, so the contract between the CQL engine
 * and the measure pipeline is testable in one place.
 */
public final class CqlExpressionValue {

    private static final CqlExpressionValue EMPTY = new CqlExpressionValue(null, null, Collections.emptySet());

    private final @Nullable String expressionName;
    private final @Nullable Object raw;
    private final Set<Value> evaluatedResources;

    private CqlExpressionValue(@Nullable String expressionName, @Nullable Object raw, Set<Value> evaluatedResources) {
        this.expressionName = expressionName;
        this.raw = raw;
        this.evaluatedResources = evaluatedResources;
    }

    /**
     * Wraps an {@link ExpressionResult}. Accepts a null result and yields an empty wrapper.
     */
    public static CqlExpressionValue of(@Nullable String expressionName, @Nullable ExpressionResult result) {
        if (result == null) {
            return EMPTY;
        }
        var resources = result.getEvaluatedResources();
        return new CqlExpressionValue(
                expressionName, result.getValue(), resources != null ? resources : Collections.emptySet());
    }

    /**
     * Wraps a raw value plus its evaluated-resource set directly. Useful for tests and
     * for callers that already hold the underlying value.
     */
    public static CqlExpressionValue ofRaw(
            @Nullable String expressionName, @Nullable Object value, @Nullable Set<Value> evaluatedResources) {
        if (value instanceof ExpressionResult expressionResult) {
            return of(expressionName, expressionResult);
        }
        return new CqlExpressionValue(
                expressionName, value, evaluatedResources != null ? evaluatedResources : Collections.emptySet());
    }

    /**
     * Returns a wrapper whose value is null and whose evaluated-resources set is empty.
     */
    public static CqlExpressionValue empty() {
        return EMPTY;
    }

    public @Nullable String expressionName() {
        return expressionName;
    }

    public boolean isNull() {
        return raw == null;
    }

    public boolean isBoolean() {
        return raw instanceof Boolean || raw instanceof org.opencds.cqf.cql.engine.runtime.Boolean;
    }

    public boolean isTrue() {
        if (raw instanceof Boolean bool) {
            return bool;
        }
        if (raw instanceof org.opencds.cqf.cql.engine.runtime.Boolean cqlBool) {
            return cqlBool.getValue();
        }
        return false;
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
        if (raw instanceof org.opencds.cqf.cql.engine.runtime.List list) {
            return !list.iterator().hasNext();
        }
        return false;
    }

    public Optional<Boolean> asBoolean() {
        return raw instanceof org.opencds.cqf.cql.engine.runtime.Boolean b
                ? Optional.of(b.getValue())
                : Optional.empty();
    }

    /**
     * Returns the underlying value as a typed {@link Map} when it is one, otherwise empty.
     * The single unchecked cast is localized here so call sites do not have to repeat it.
     * Used for arbitrary CQL Map values (e.g. supporting evidence, formatting). For
     * measure-observation accumulators produced by
     * {@code FunctionEvaluationHandler.processMeasureObservation}, prefer
     * {@link #asObservationAccumulator()}.
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<Object, Object>> asMap() {
        return raw instanceof Map<?, ?> map ? Optional.of((Map<Object, Object>) map) : Optional.empty();
    }

    /**
     * Returns the underlying value as the {@link ObservationAccumulator} produced by
     * {@code FunctionEvaluationHandler.processMeasureObservation}, or empty otherwise.
     * The accumulator wraps a {@code List<ObservationEntry>} in a non-Iterable record so the
     * upstream {@link #asIterable()} path doesn't unroll it into individual entries.
     */
    public Optional<ObservationAccumulator> asObservationAccumulator() {
        return raw instanceof ObservationAccumulator acc ? Optional.of(acc) : Optional.empty();
    }

    /**
     * Returns the underlying value as the {@link FunctionResultAccumulator} produced by
     * {@code FunctionEvaluationHandler.processNonSubValueStratifier}, or empty otherwise.
     * Mirrors {@link #asObservationAccumulator()}: non-Iterable record so the upstream
     * {@link #asIterable()} path doesn't unroll it into individual entries.
     */
    public Optional<FunctionResultAccumulator> asFunctionResultAccumulator() {
        return raw instanceof FunctionResultAccumulator acc ? Optional.of(acc) : Optional.empty();
    }

    /**
     * Returns the function-result entries for a NON_SUBJECT_VALUE stratifier component, accepting
     * both the typed {@link FunctionResultAccumulator} and a raw {@code Map<inputParam, functionOutput>}.
     * <p>
     * The accumulator is the modern shape produced by
     * {@code FunctionEvaluationHandler.processNonSubValueStratifier}, but a function result may also
     * arrive as a raw {@link Map} (e.g. populated directly via
     * {@code StratifierComponentDef.putResult(subject, expression, value, resources)} or by other
     * evaluation paths). Unlike {@link #asFunctionResultAccumulator()} — whose contract is strictly
     * "only an accumulator" — this accessor normalizes both shapes so stratifier-table consumers
     * produce one row per (input, output) entry instead of a single row keyed by {@code Map.toString()}.
     * Returns empty for any other value.
     */
    public Optional<List<FunctionResultEntry>> functionResultEntries() {
        if (raw instanceof FunctionResultAccumulator acc) {
            return Optional.of(acc.entries());
        }
        if (raw instanceof Map<?, ?> map) {
            List<FunctionResultEntry> entries = map.entrySet().stream()
                    .map(entry -> new FunctionResultEntry(entry.getKey(), entry.getValue()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            return Optional.of(entries);
        }
        return Optional.empty();
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
        if (raw instanceof Iterable<?> iterable) {
            return (Iterable<Object>) iterable;
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
        if (raw instanceof Iterable<?> iterable) {
            return (Iterable<Object>) iterable;
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
        if (raw instanceof org.opencds.cqf.cql.engine.runtime.Boolean bool) {
            if (!bool.getValue()) {
                return Collections.emptyList();
            }
            var subjectResult = evaluationResult.get(subjectType);
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
    @SuppressWarnings("unchecked")
    public Set<Object> valueAsSet() {
        if (raw == null) {
            return new HashSetForFhirResourcesAndCqlTypes<>();
        }
        if (raw instanceof Iterable<?> iterable) {
            return new HashSetForFhirResourcesAndCqlTypes<>((Iterable<Object>) iterable);
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

    public Set<Value> evaluatedResources() {
        return evaluatedResources;
    }

    public @Nullable Object raw() {
        return raw;
    }
}
