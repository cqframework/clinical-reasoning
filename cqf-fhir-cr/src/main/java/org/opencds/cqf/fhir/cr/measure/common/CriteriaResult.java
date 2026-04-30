package org.opencds.cqf.fhir.cr.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.opencds.cqf.cql.engine.runtime.Value;

public class CriteriaResult {
    private final Value value;
    private final Set<Value> evaluatedResources;

    public static final Value NULL_VALUE = null;

    public static final CriteriaResult EMPTY_RESULT = new CriteriaResult(NULL_VALUE, Collections.emptySet());

    public CriteriaResult(Value value, Set<Value> evaluatedResources) {
        this.value = value;
        this.evaluatedResources = new HashSet<>(evaluatedResources);
    }

    public Value rawValue() {
        return this.value;
    }

    public Iterable<Value> iterableValue() {
        if (this.rawValue() instanceof org.opencds.cqf.cql.engine.runtime.List cqlList) {
            return cqlList.getValue();
        } else if (this.rawValue() == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(this.rawValue());
        }
    }

    public Set<Value> valueAsSet() {
        if (this.rawValue() instanceof org.opencds.cqf.cql.engine.runtime.List cqlList) {
            return buildSet(unsafeCast(cqlList.getValue()));
        } else if (this.rawValue() == null) {
            return new HashSetForFhirResourcesAndCqlTypes<>();
        } else {
            return new HashSetForFhirResourcesAndCqlTypes<>(this.rawValue());
        }
    }

    public Set<Value> evaluatedResources() {
        return this.evaluatedResources;
    }

    /**
     * Returns the value(s) as a list with nulls filtered out.
     * Handles single values, iterables, and null values uniformly.
     */
    public List<Value> nonNullValues() {
        if (this.value == null) {
            return Collections.emptyList();
        }
        if (this.value instanceof org.opencds.cqf.cql.engine.runtime.List iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .filter(Objects::nonNull)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
        return List.of(this.value);
    }

    private Set<Value> buildSet(Iterable<Value> iterable) {
        return new HashSetForFhirResourcesAndCqlTypes<>(iterable);
    }

    @SuppressWarnings("unchecked")
    private static <T> T unsafeCast(Object object) {
        return (T) object;
    }
}
