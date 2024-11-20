package org.opencds.cqf.fhir.cr.measure;

/**
 * All options that are pertinent to {@link org.opencds.cqf.fhir.cr.measure.common.SubjectProvider}
 * implementations, such as {@link org.opencds.cqf.fhir.cr.measure.r4.R4RepositorySubjectProvider}.
 */
public class SubjectProviderOptions {

    private boolean isPartOfEnabled;

    /**
     * @return true if clinical-reasoning is configured to resolve subject Organization queries
     * in which Organizations related by partOf when compiling the list of Patients related by
     * managingOrganization.
     * <p/>
     * false to disregard any Organizations and patients related by partOf.
     */
    public boolean isPartOfEnabled() {
        return isPartOfEnabled;
    }

    public SubjectProviderOptions setPartOfEnabled(boolean partOfEnabled) {
        isPartOfEnabled = partOfEnabled;
        return this;
    }
}
