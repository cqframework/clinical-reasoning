package org.opencds.cqf.fhir.cr.measure;

// LUKETODO:  javadoc
public class SubjectProviderOptions {

    private boolean isPartOfEnabled;

    public boolean isPartOfEnabled() {
        return isPartOfEnabled;
    }

    public SubjectProviderOptions setPartOfEnabled(boolean thePartOfEnabled) {
        isPartOfEnabled = thePartOfEnabled;
        return this;
    }
}
