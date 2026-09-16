package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opencds.cqf.cql.engine.runtime.Value;

public class StratifierComponentDef {
    private final String id;
    private final ConceptDef code;
    private final String expression;

    private Map<String, CqlExpressionValue> results;

    public StratifierComponentDef(String id, ConceptDef code, String expression) {
        this.id = id;
        this.code = code;
        this.expression = expression;
    }

    public String id() {
        return this.id;
    }

    public String expression() {
        return this.expression;
    }

    public ConceptDef code() {
        return this.code;
    }

    public void putResult(String subject, CqlExpressionValue expressionValue) {
        this.getResults().put(subject, expressionValue);
    }

    public void putResult(String subject, String expression, Object value, Set<Value> evaluatedResources) {
        this.putResult(subject, CqlExpressionValue.ofRaw(expression, value, evaluatedResources));
    }

    public Map<String, CqlExpressionValue> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }
}
