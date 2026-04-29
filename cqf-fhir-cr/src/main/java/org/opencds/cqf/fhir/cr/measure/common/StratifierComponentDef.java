package org.opencds.cqf.fhir.cr.measure.common;

import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.cql.engine.runtime.Value;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public void putResult(String subject, Value value, Set<Value> evaluatedResources) {
        this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }
}
