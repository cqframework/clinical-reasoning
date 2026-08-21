package org.opencds.cqf.fhir.cql.engine.terminology

/*
* There are two general terminology operations the the CQL engine uses.
* 1. Terminology expansion - given a ValueSet url, calculate the set of codes in the value set.
*      This can be used by CQL authors inline in CQL, but is primarily used to filter resources
*      like Observations and Conditions that are a member some code set.
* 2. code valueset membership - give a code, check to see if it's a member of some value set.
  3. code lookup - given a code, find the code details in a code system
*/
class TerminologySettings {

    /**
     * How to treat pre-expanded value sets. If a value set is not pre-expanded, fall back to the
     * expansion behavior defined by the expansion mode.
     */
    enum class VALUESET_PRE_EXPANSION_MODE {
        /** Require value sets to be pre-expanded, never perform expansion operations */
        REQUIRE,

        /**
         * Use a pre-expansion if it's present on the value set resource, other perform an expansion
         */
        USE_IF_PRESENT,

        /** Ignore any pre-expansion and always perform an expansion */
        IGNORE,
    }

    /** How to do an expansions */
    enum class VALUESET_EXPANSION_MODE {
        // Decreasing order of performance

        /** Detect from capability statements */
        AUTO,

        /** Repository performs expansion - Force the use of the $expand operation */
        USE_EXPAND_OPERATION,

        /** CQL engine performs expansion - Make a best effort expansion in the CQL engine */
        PERFORM_NAIVE_EXPANSION,
    }

    /** How to do valueset code membership */
    enum class VALUESET_MEMBERSHIP_MODE {
        // Decreasing order of performance

        /** Detect from capability statements */
        AUTO,

        /**
         * Repository performs membership operation - Force the use of the $validate-code operation
         */
        USE_VALIDATE_CODE_OPERATION,

        /**
         * CQL engine checks code membership by expanding the value set and checking for the code in
         * the expansion
         */
        USE_EXPANSION,
    }

    /** How to do code lookups */
    enum class CODE_LOOKUP_MODE {
        // Decreasing order of performance

        /** Detect from capability statements */
        AUTO,

        /**
         * Repository performs membership operation - Force the use of the $validate-code operation
         */
        USE_VALIDATE_CODE_OPERATION,

        /**
         * CQL engine checks code membership by doing a simple check of the code's declared system
         * against the expected url
         */
        USE_CODESYSTEM_URL,
    }

    var valuesetExpansionMode: VALUESET_EXPANSION_MODE = VALUESET_EXPANSION_MODE.AUTO
        private set

    var valuesetMembershipMode: VALUESET_MEMBERSHIP_MODE = VALUESET_MEMBERSHIP_MODE.AUTO
        private set

    var codeLookupMode: CODE_LOOKUP_MODE = CODE_LOOKUP_MODE.AUTO
        private set

    var valuesetPreExpansionMode: VALUESET_PRE_EXPANSION_MODE =
        VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT
        private set

    /*
     * Default constructor for TerminologySettings
     */
    constructor()

    /**
     * Copy constructor for TerminologySettings
     *
     * @param terminologySettings
     */
    constructor(terminologySettings: TerminologySettings) {
        this.valuesetExpansionMode = terminologySettings.valuesetExpansionMode
        this.valuesetMembershipMode = terminologySettings.valuesetMembershipMode
        this.codeLookupMode = terminologySettings.codeLookupMode
        this.valuesetPreExpansionMode = terminologySettings.valuesetPreExpansionMode
    }

    fun setValuesetExpansionMode(
        valuesetExpansionMode: VALUESET_EXPANSION_MODE
    ): TerminologySettings {
        this.valuesetExpansionMode = valuesetExpansionMode
        return this
    }

    fun setValuesetMembershipMode(
        valuesetMembershipMode: VALUESET_MEMBERSHIP_MODE
    ): TerminologySettings {
        this.valuesetMembershipMode = valuesetMembershipMode
        return this
    }

    fun setCodeLookupMode(codeLookupMode: CODE_LOOKUP_MODE): TerminologySettings {
        this.codeLookupMode = codeLookupMode
        return this
    }

    fun setValuesetPreExpansionMode(
        valuesetPreExpansionMode: VALUESET_PRE_EXPANSION_MODE
    ): TerminologySettings {
        this.valuesetPreExpansionMode = valuesetPreExpansionMode
        return this
    }
}
