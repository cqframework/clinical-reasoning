package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CriteriaResult {
    private final Object value;
    private final List<Object> evaluatedResources;

    public static final Object NULL_VALUE = new Object();

    public static final CriteriaResult EMPTY_RESULT = new CriteriaResult(NULL_VALUE, Collections.emptyList());

    public CriteriaResult(Object value, List<Object> evaluatedResources) {
        this.value = value;
        this.evaluatedResources = new ArrayList<>(evaluatedResources);
    }

    public Object rawValue() {
        return this.value;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterable<Object> iterableValue() {
        if (this.rawValue() instanceof Iterable<?>) {
            return (Iterable) this.rawValue();
        } else if (this.rawValue() == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(this.rawValue());
        }
    }

    public List<Object> evaluatedResources() {
        return this.evaluatedResources;
    }
}
