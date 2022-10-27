package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StratifierDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;

    private final List<StratifierComponentDef> components;

    private Map<String, Object> subjectValues;

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

    public void putSubjectValue(String subject, Object value) {
        this.getSubjectValues().put(subject, value);
    }

    public Map<String, Object> getSubjectValues() {
        if (this.subjectValues == null) {
            this.subjectValues = new HashMap<>();
        }

        return this.subjectValues;
    }

    public List<StratifierComponentDef> components() {
        return this.components;
    }

}
