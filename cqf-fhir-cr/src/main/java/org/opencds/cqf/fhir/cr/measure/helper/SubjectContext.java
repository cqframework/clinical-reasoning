package org.opencds.cqf.fhir.cr.measure.helper;

public class SubjectContext {
    public SubjectContext(String contextType, Object contextValue) {
        this.contextType = contextType;
        this.contextValue = contextValue;
    }

    private String contextType;
    public String getContextType() {
        return contextType;
    }

    private Object contextValue;
    public Object getContextValue() {
        return contextValue;
    }
}
