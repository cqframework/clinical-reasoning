package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

public class CriteriaResult {
    private final Object value;
    private final Set<Object> evaluatedResources;

    public static final Object NULL_VALUE = new Object();

    public static final CriteriaResult EMPTY_RESULT = new CriteriaResult(NULL_VALUE, Collections.emptySet());

    public CriteriaResult(Object value, Set<Object> evaluatedResources) {
        this.value = value;
        this.evaluatedResources = new HashSet<>(evaluatedResources);
    }

    public Object rawValue() {
        return this.value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Iterable<Object> iterableValue() {
        if (this.rawValue() instanceof Iterable<?>) {
            return (Iterable) this.rawValue();
        } else if (this.rawValue() == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(this.rawValue());
        }
    }

    public Set<Object> valueAsSet() {
        if (this.rawValue() instanceof Iterable) {
            return buildSet(unsafeCast(this.rawValue()));
        } else if (this.rawValue() == null) {
            return new HashSetForFhirResourcesAndCqlTypes<>();
        } else {
            return new HashSetForFhirResourcesAndCqlTypes<>(this.rawValue());
        }
    }

    public Set<Object> evaluatedResources() {
        return this.evaluatedResources;
    }

    /**
     * Returns the value(s) as a list with nulls filtered out.
     * Handles single values, iterables, and null values uniformly.
     */
    public List<Object> nonNullValues() {
        if (this.value == null) {
            return Collections.emptyList();
        }
        if (this.value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .filter(Objects::nonNull)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return List.of(this.value);
    }

    private Set<Object> buildSet(Iterable<Object> iterable) {
        return new HashSetForFhirResourcesAndCqlTypes<>(iterable);
    }

    @SuppressWarnings("unchecked")
    private static <T> T unsafeCast(Object object) {
        return (T) object;
    }
}
