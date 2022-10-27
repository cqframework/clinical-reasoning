package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.HashMap;
import java.util.Map;

public class StratifierComponentDef {
    private final String id;
    private final ConceptDef code;
    private final String expression;

    private Map<String, Object> subjectValues;

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

    public void putSubjectValue(String subject, Object value) {
        this.getSubjectValues().put(subject, value);
    }

    public Map<String, Object> getSubjectValues() {
        if (this.subjectValues == null) {
            this.subjectValues = new HashMap<>();
        }

        return this.subjectValues;
    }
}
