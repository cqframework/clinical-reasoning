package org.opencds.cqf.fhir.cql.engine.parameters;

import org.opencds.cqf.cql.engine.runtime.Value;

public class CqlParameterDefinition {

    public CqlParameterDefinition(String name, String type, Boolean isList) {
        this(name, type, isList, null);
    }

    public CqlParameterDefinition(String name, String type, Boolean isList, Value value) {
        this.name = name;
        this.type = type;
        this.isList = isList;
        this.value = value;
    }

    private String name;
    private String type;
    private Value value;
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

    public Value getValue() {
        return this.value;
    }
}
