package org.opencds.cqf.fhir.cr.measure.helper;

public class SubjectContext {
    public SubjectContext(String contextType, String contextValue) {
        this.contextType = contextType;
        this.contextValue = contextValue;
    }

    private String contextType;

    public String getContextType() {
        return contextType;
    }

    private String contextValue;

    public String getContextValue() {
        return contextValue;
    }
}
