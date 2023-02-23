package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CriteriaResult {
    private final Object value;
    private final Set<Object> evaluatedResources;

    public static final Object NULL_VALUE = new Object();

    public static final CriteriaResult EMPTY_RESULT =
            new CriteriaResult(NULL_VALUE, Collections.emptySet());

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

    public Set<Object> evaluatedResources() {
        return this.evaluatedResources;
    }
}
