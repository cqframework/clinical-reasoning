package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StratifierDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;

    private final List<StratifierComponentDef> components;

    @Nullable
    private Map<String, CriteriaResult> results;

    public StratifierDef(String id, ConceptDef code, String expression) {
        this(id, code, expression, Collections.emptyList());
    }

    public StratifierDef(String id, ConceptDef code, String expression, List<StratifierComponentDef> components) {
        this.id = id;
        this.code = code;
        this.expression = expression;
        this.components = components;
    }

    public String expression() {
        return this.expression;
    }

    public ConceptDef code() {
        return this.code;
    }

    public String id() {
        return this.id;
    }

    public List<StratifierComponentDef> components() {
        return this.components;
    }

    public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
        this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }

    public Set<?> getAllCriteriaResultValues() {
        return this.getResults().values().stream()
                .map(CriteriaResult::rawValue)
                .map(this::toSet)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<?> toSet(Object value) {
        if (value == null) {
            return Set.of();
        }

        if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toUnmodifiableSet());
        } else {
            return Set.of(value);
        }
    }

    @Nullable
    public Class<?> getResultType() {
        if (this.results == null || this.results.isEmpty()) {
            return null;
        }

        var resultClasses = results.values().stream()
                .map(CriteriaResult::rawValue)
                .map(this::extractClassesFromSingleOrListResult)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableSet());

        if (resultClasses.size() == 1) {
            return resultClasses.iterator().next();
        }

        if (resultClasses.isEmpty()) {
            return null;
        }

        // LUKETODO:  do we ever get this?
        throw new RuntimeException("There should be only one result type for this StratifierDef but there was: %s"
                .formatted(resultClasses));
    }

    // LUKETODO:  code duplication
    private List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (!(result instanceof Iterable<?> iterable)) {
            return Collections.singletonList(result.getClass());
        }

        // Need to this to return List<Class<?>> and get rid of Sonar warnings.
        final Stream<Class<?>> classStream =
                getStream(iterable).filter(Objects::nonNull).map(Object::getClass);

        return classStream.toList();
    }

    private Stream<?> getStream(Iterable<?> iterable) {
        if (iterable instanceof List<?> list) {
            return list.stream();
        }

        // It's entirely possible CQL returns an Iterable that is not a List, so we need to handle that case
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
