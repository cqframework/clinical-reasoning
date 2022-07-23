package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.List;

public class SdeDef {

    private String id;
    private String expression;
    private String code;
    private boolean hasCode;
    private String system;
    private String display;
    private String text;


    private List<Object> values;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExpression() {
        return this.expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean hasCode() { return hasCode; }

    public void setHasCode(boolean hasCode) { this.hasCode = hasCode; }

    public String getSystem() { return system; }

    public void setSystem(String system) { this.system = system; }

    public String getDisplay() { return display; }

    public void setDisplay(String display) { this.display = display; }

    public String getText() { return text; }

    public void setText(String text) { this.text = text; }

    public List<Object> getValues() {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }

        return this.values;
    }
}
