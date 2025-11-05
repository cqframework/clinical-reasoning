package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class StratifierComponentDef {
    private final String id;
    private final ConceptDef code;
    private final String expression;

    private Map<String, CriteriaResult> results;

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

    public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
        this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StratifierComponentDef.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("code=" + code)
                .add("expression='" + expression + "'")
                .add("results=" + results)
                .toString();
    }
}
