package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.List;

public class SdeDef {

    private String expression;
    private String code;

    private List<Object> values;

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

    public List<Object> getValues() {
        if (this.values == null) {
            this.values = new ArrayList<>();
        }

        return this.values;
    }
}
