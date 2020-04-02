package org.opencds.cqf.cql.evaluator;

public class ParameterInfo {
    public ParameterInfo() {}
    public ParameterInfo(String libraryName, String identifier, Object value) {
        this.libraryName = libraryName;
        this.identifier = identifier;
        this.value = value;
    }

    private String libraryName;

    public String getLibraryName() {
        return this.libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    private String identifier;

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    private Object value;

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}