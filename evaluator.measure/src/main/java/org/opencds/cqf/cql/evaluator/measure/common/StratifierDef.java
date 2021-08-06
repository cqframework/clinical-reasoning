package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StratifierDef {

    private String expression;
    private String code;

    private Map<Object, Set<String>> values;

    private List<StratifierComponentDef> stratifierComponentDefs;

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

    public Map<Object, Set<String>> getValues() {
        if (this.values == null) {
            this.values = new HashMap<>();
        }

        return this.values;
    }

    public List<StratifierComponentDef> getComponents() {
        if (this.stratifierComponentDefs == null) {
            this.stratifierComponentDefs = new ArrayList<>();
        }

        return this.stratifierComponentDefs;
    }
    
}
