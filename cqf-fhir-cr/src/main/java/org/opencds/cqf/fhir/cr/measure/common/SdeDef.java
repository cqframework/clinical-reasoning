package org.opencds.cqf.fhir.cr.measure.common;

public class SdeDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;
    private final String description;

    public SdeDef(String id, ConceptDef code, String expression) {
        this(id, code, expression, null);
    }

    public SdeDef(String id, ConceptDef code, String expression, String description) {
        this.id = id;
        this.code = code;
        this.expression = expression;
        this.description = description;
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

    public String description() {
        return this.description;
    }
}
