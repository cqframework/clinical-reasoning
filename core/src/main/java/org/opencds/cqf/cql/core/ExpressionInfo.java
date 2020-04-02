package org.opencds.cqf.cql.evaluator;

public class ExpressionInfo {
    public ExpressionInfo() {}
    public ExpressionInfo(String libraryName, String identifier) {
        this.libraryName = libraryName;
        this.identifier = identifier;
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
}