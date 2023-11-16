package org.opencds.cqf.fhir.cql.engine.retrieve;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;

public class RetrieveSettings {

    public RetrieveSettings() {
        this(new TerminologySettings());
    }

    public RetrieveSettings(TerminologySettings terminologySettings) {
        this.terminologySettings = terminologySettings;
    }

    private final TerminologySettings terminologySettings;
    private SEARCH_PARAMETER_MODE searchParameterMode = SEARCH_PARAMETER_MODE.AUTO;
    private PROFILE_MODE profileMode = PROFILE_MODE.OFF;
    private TERMINOLOGY_PARAMETER_MODE terminologyParameterMode = TERMINOLOGY_PARAMETER_MODE.AUTO;

    public TerminologySettings getTerminologySettings() {
        return terminologySettings;
    }

    // Decreasing order of performance
    // Applies to all search parameters
    // EXCEPT terminology search parameters, which are controlled by the terminology settings.
    public enum SEARCH_PARAMETER_MODE {
        // Detect from capability statements, and make best guess in the absence of capability statements.
        AUTO,
        // Force use of repository search parameters
        REPOSITORY,
        // Force use of CQL filtering
        CQL
    }

    public SEARCH_PARAMETER_MODE getSearchParameterMode() {
        return searchParameterMode;
    }

    /**
     * Applies to all search parameters EXCEPT terminology search parameters.
     * @param searchParameterMode whether to use repository search parameters or CQL filtering
     * @return this
     */
    public RetrieveSettings setSearchParameterMode(SEARCH_PARAMETER_MODE searchParameterMode) {
        this.searchParameterMode = searchParameterMode;
        return this;
    }

       // How to do a filter, if and when we need a filter.
    // e.g. Observation O where O.code ~ "ValueSet"
    public enum TERMINOLOGY_PARAMETER_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Use code:in=valueSetUrl - Force the use of the in modifier of the search parameter to filter resources.
        REPOSITORY,
        // Use code=system|value,system|value - Force the use of the the relevant search parameter WITHOUT the in modifier to filter resources.
        INLINE,
        // CQL engine does the filter - Retrieve all potentially applicable resources and apply terminology filtering in the CQL engine.
        CQL
    }

    /**
     * Applies ONLY to terminology search parameters
     * @param terminologyParameterMode mode to use for terminology search parameters
     * @return this
     */
    public RetrieveSettings setTerminologyParameterMode(TERMINOLOGY_PARAMETER_MODE terminologyParameterMode) {
        this.terminologyParameterMode = terminologyParameterMode;
        return this;
    }

    public TERMINOLOGY_PARAMETER_MODE getTerminologyParameterMode() {
        return terminologyParameterMode;
    }

    // TODO: Profiles are completely unsupported for now. This just lays out some potential options for doing that
    public enum PROFILE_MODE {
        // Always check the resource profile by validating the returned resource against the profile
        // This requires access to the structure defs that define the profile at runtime
        // Meaning, they need to be loaded on the server or otherwise. If they are unavailable, it's an automatic
        // failure.
        ENFORCED,
        // Same as above, but don't error if you don't have access to the profiles at runtime
        OPTIONAL,
        // Check that the resources declare the profile they conform too (generally considered a bad practice)
        DECLARED,
        // Let the underlying repository validate profiles (IOW, offload validation)
        TRUST,
        // Don't check resource profile, even if specified by the engine
        OFF
    }

    public PROFILE_MODE getProfileMode() {
        return this.profileMode;
    }

    public RetrieveSettings setProfileMode(PROFILE_MODE profileMode) {
        this.profileMode = profileMode;
        return this;
    }
}
