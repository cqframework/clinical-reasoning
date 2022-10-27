package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.List;

public class ConceptDef {

    private final List<CodeDef> codes;
    private final String text;

    public ConceptDef(List<CodeDef> codes, String text) {
        this.codes = codes;
        this.text = text;
    }

    public List<CodeDef> codes() {
        return this.codes;
    }

    public boolean isEmpty() {
        return this.codes.isEmpty();
    }

    public CodeDef first() {
        if (isEmpty()) {
            throw new IllegalStateException("No codes in this concept, so can't return first()");
        }

        return this.codes.get(0);
    }

    public String text() {
        return this.text;
    }
}
