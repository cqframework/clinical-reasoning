package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.Collections;
import java.util.List;

public class CriteriaResult {
    private final Object value;
    private final List<Object> evaluatedResources;

    public static final Object NULL_VALUE = new Object();

    public static final CriteriaResult EMPTY_RESULT = new CriteriaResult(NULL_VALUE, Collections.emptyList());

    public CriteriaResult(Object value, List<Object> evaluatedResources) {
        this.value = value;
        this.evaluatedResources = List.copyOf(evaluatedResources);
    }

    public Object value() {
        return this.value;
    }

    public List<Object> evaluatedResources() {
        return this.evaluatedResources;
    }
}
