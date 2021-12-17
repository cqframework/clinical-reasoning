package org.opencds.cqf.cql.evaluator.library;

public class CqlParameterDefinition {


    public CqlParameterDefinition(String name, String type, Boolean isList) {
        this(name, type, isList, null);
    }

    public CqlParameterDefinition(String name, String type, Boolean isList, Object value) {
        this.name = name;
        this.type = type;
        this.isList = isList;
        this.value = value;
    }

    private String name;
    private String type;
    private Object value;
    private Boolean isList;
    
    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public Boolean getIsList() {
        return this.isList;
    }

    public Object getValue() {
        return this.value;
    }
    
}
