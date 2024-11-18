package org.opencds.cqf.fhir.cr.measure;

/**
 * All options that are pertinent to {@link org.opencds.cqf.fhir.cr.measure.common.SubjectProvider}
 * implementations, such as {@link org.opencds.cqf.fhir.cr.measure.r4.R4RepositorySubjectProvider}.
 */
public class SubjectProviderOptions {

    private boolean isPartOfEnabled;

    public boolean isPartOfEnabled() {
        return isPartOfEnabled;
    }

    public SubjectProviderOptions setPartOfEnabled(boolean partOfEnabled) {
        isPartOfEnabled = partOfEnabled;
        return this;
    }
}
