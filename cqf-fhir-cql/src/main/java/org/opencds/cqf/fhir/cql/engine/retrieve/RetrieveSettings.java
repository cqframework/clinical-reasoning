package org.opencds.cqf.fhir.cql.engine.retrieve;

import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.PROFILE_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.TERMINOLOGY_MODE;

public class RetrieveSettings {
    private PROFILE_MODE profileMode = PROFILE_MODE.OFF;
    private FILTER_MODE filterMode = FILTER_MODE.AUTO;
    private TERMINOLOGY_MODE terminologyMode = TERMINOLOGY_MODE.AUTO;

    public FILTER_MODE getFilterMode() {
        return this.filterMode;
    }

    public RetrieveSettings setFilterMode(FILTER_MODE filterMode) {
        this.filterMode = filterMode;
        return this;
    }

    public PROFILE_MODE getProfileMode() {
        return profileMode;
    }

    public RetrieveSettings setProfileMode(PROFILE_MODE profileMode) {
        this.profileMode = profileMode;
        return this;
    }

    public TERMINOLOGY_MODE getTerminologyMode() {
        return this.terminologyMode;
    }

    public RetrieveSettings setTerminologyMode(TERMINOLOGY_MODE terminologyMode) {
        this.terminologyMode = terminologyMode;
        return this;
    }
}
