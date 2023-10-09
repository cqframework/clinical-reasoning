package org.opencds.cqf.fhir.cql.engine.retrieve;

import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.PROFILE_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.BaseRetrieveProvider.TERMINOLOGY_MODE;

/*
 * This class sets some options for how the CQL engine handles retrieves. By default, it's set for maximum compatibility.
 * This means rather offload certain operations to the underlying data (and/or terminology) source, it performs the operation
 * itself. In certain cases this can be extremely detrimental for performance, such as when a database index is available for doing
 * a filter to a specific code codesystem.
 *
 * Changing the filterMode to AUTO or REPOSITORY can provide dramatic speedups. Similarly, changing terminologyMode to AUTO or INLINE
 * can provide similar increases in performance. The relevant repository needs to support the operations that are being offloaded
 * for this to work as expected.
 *
 * Further work needs to be done to ensure that the CQL engine automatically detects and optimizes the offloading. This is an on-going
 * effort.
 */
public class RetrieveSettings {
    private PROFILE_MODE profileMode = PROFILE_MODE.OFF;
    private FILTER_MODE filterMode = FILTER_MODE.INTERNAL;
    private TERMINOLOGY_MODE terminologyMode = TERMINOLOGY_MODE.EXPAND;

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
