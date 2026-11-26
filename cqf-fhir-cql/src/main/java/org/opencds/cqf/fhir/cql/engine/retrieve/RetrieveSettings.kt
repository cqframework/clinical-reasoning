package org.opencds.cqf.fhir.cql.engine.retrieve

class RetrieveSettings {
    var searchParameterMode: SEARCH_FILTER_MODE = SEARCH_FILTER_MODE.AUTO
        private set

    var profileMode: PROFILE_MODE = PROFILE_MODE.OFF
        private set

    var terminologyParameterMode: TERMINOLOGY_FILTER_MODE = TERMINOLOGY_FILTER_MODE.AUTO
        private set

    var conversionMode: CONVERSION_MODE = CONVERSION_MODE.AUTO
        private set

    /** Default constructor for RetrieveSettings */
    constructor()

    /**
     * Copy constructor for RetrieveSettings
     *
     * @param retrieveSettings
     */
    constructor(retrieveSettings: RetrieveSettings) {
        this.searchParameterMode = retrieveSettings.searchParameterMode
        this.profileMode = retrieveSettings.profileMode
        this.terminologyParameterMode = retrieveSettings.terminologyParameterMode
        this.conversionMode = retrieveSettings.conversionMode
    }

    /**
     * Applies to all search parameters EXCEPT terminology search parameters, which are controlled
     * by the terminology settings.
     */
    enum class SEARCH_FILTER_MODE {

        // Decreasing order of performance

        /**
         * Detect from capability statements, and make best guess in the absence of capability
         * statements.
         */
        AUTO,

        /** Force use of repository search parameters */
        USE_SEARCH_PARAMETERS,

        /** Force use of CQL filtering */
        FILTER_IN_MEMORY,
    }

    /**
     * How to do a filter, if and when we need a filter. e.g. Observation O where O.code ~
     * "ValueSet"
     */
    enum class TERMINOLOGY_FILTER_MODE {
        // Decreasing order of performance

        /** Detect from capability statements */
        AUTO,

        /**
         * Use code:in=valueSetUrl - Force the use of the :in modifier of the search parameter to
         * filter resources.
         */
        USE_VALUE_SET_URL,

        /**
         * Use code=system|value,system|value - Force the use of the the relevant search parameter
         * WITHOUT the in modifier to filter resources.
         */
        USE_INLINE_CODES,

        /**
         * CQL engine does the filter - Retrieve all potentially applicable resources and apply
         * terminology filtering in the CQL engine.
         */
        FILTER_IN_MEMORY,
    }

    enum class PROFILE_MODE {
        /**
         * Always check the resource profile by validating the returned resource against the
         * profile. This requires access to the structure defs that define the profile at runtime.
         * Meaning, they need to be loaded on the server or otherwise. If they are unavailable, it's
         * an automatic failure. ENFORCED is not yet implemented.
         */
        ENFORCED,

        /**
         * Use the declared profile (generally considered a bad practice). If the resource doesn't
         * declare a profile, it's filtered out.
         */
        DECLARED,

        /** Same as above, but if the resource doesn't declare a profile, it's not filtered out. */
        OPTIONAL,

        /** Let the underlying repository validate profiles (IOW, offload validation) */
        TRUST,

        /** Don't check resource profile, even if specified by the engine */
        OFF,
    }

    /**
     * Whether retrieved FHIR resources are converted to CQL values eagerly (every element up front)
     * or lazily (each element on first access). Lazy conversion restores engine 4.6.0 "convert only
     * what you read" behavior and avoids materializing whole compartments, but it reads resource
     * fields at access time, so it is only safe when the repository returns value-stable resources
     * (not mutated or recycled after retrieval).
     */
    enum class CONVERSION_MODE {
        /** Lazy when the repository is known to return value-stable resources, otherwise eager. */
        AUTO,

        /** Always convert every element up front (safe for any repository). */
        EAGER,

        /** Always convert lazily. Only use for a repository known to return stable resources. */
        LAZY,
    }

    /**
     * Applies to all search parameters EXCEPT terminology search parameters.
     *
     * @param searchParameterMode whether to use repository search parameters or CQL filtering
     * @return this
     */
    fun setSearchParameterMode(searchParameterMode: SEARCH_FILTER_MODE): RetrieveSettings {
        this.searchParameterMode = searchParameterMode
        return this
    }

    /**
     * Applies ONLY to terminology search parameters
     *
     * @param terminologyParameterMode mode to use for terminology search parameters
     * @return this
     */
    fun setTerminologyParameterMode(
        terminologyParameterMode: TERMINOLOGY_FILTER_MODE
    ): RetrieveSettings {
        this.terminologyParameterMode = terminologyParameterMode
        return this
    }

    fun setProfileMode(profileMode: PROFILE_MODE): RetrieveSettings {
        this.profileMode = profileMode
        return this
    }

    fun setConversionMode(conversionMode: CONVERSION_MODE): RetrieveSettings {
        this.conversionMode = conversionMode
        return this
    }
}
