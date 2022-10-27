package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.List;

public class SdeDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;

    public SdeDef(String id, ConceptDef code, String expression) {
        this.id = id;
        this.code = code;
        this.expression = expression;
    }


    private List<Object> values;

    public String id() {
        return this.id;
    }

    public String expression() {
        return this.expression;
    }

    public ConceptDef code() {
        return this.code;
    }

    public void addValue(Object value) {
        this.getValues().add(value);
    }

    public List<Object> getValues() {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }

        return this.values;
    }
}
