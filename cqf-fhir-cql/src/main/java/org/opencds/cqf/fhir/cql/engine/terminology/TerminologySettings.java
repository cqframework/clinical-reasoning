package org.opencds.cqf.fhir.cql.engine.terminology;

/*
* There are two general terminology operations the the CQL engine uses.
* 1. Terminology expansion - given a ValueSet url, calculate the set of codes in the value set.
*      This can be used by CQL authors inline in CQL, but is primarily used to filter resources
*      like Observations and Conditions that are a member some code set.
* 2. code valueset membership - give a code, check to see if it's a member of some value set.
  3. code lookup - given a code, find the code details in a code system
*/
public class TerminologySettings {

    // How to treat pre-expanded value sets. If a value set is not pre-expanded, fall back to the expansion behavior
    // defined by the expansion mode.
    public enum VALUESET_PRE_EXPANSION_MODE {
        // Require value sets to be pre-expanded, never perform expansion operations
        REQUIRE,
        // Use a pre-expansion if it's present on the value set resource, other perform an expansion
        USE_IF_PRESENT,
        // Ignore any pre-expansion and always perform an expansion
        IGNORE
    }

    // How to do an expansions
    public enum VALUESET_EXPANSION_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Repository performs expansion - Force the use of the $expand operation
        USE_EXPAND_OPERATION,
        // CQL engine performs expansion - Make a best effort expansion in the CQL engine
        PERFORM_NAIVE_EXPANSION
    }

    // How to do valueset code membership
    public enum VALUESET_MEMBERSHIP_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Repository performs membership operation - Force the use of the $validate-code operation
        USE_VALIDATE_CODE_OPERATION,
        // CQL engine checks code membership by expanding the value set and checking for the code in the expansion
        USE_EXPANSION
    }

    // How to do code lookups
    public enum CODE_LOOKUP_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Repository performs membership operation - Force the use of the $validate-code operation
        USE_VALIDATE_CODE_OPERATION,
        // CQL engine checks code membership by doing a simple check of the code's declared system against the expected
        // url
        USE_CODESYSTEM_URL
    }

    private VALUESET_EXPANSION_MODE valuesetExpansionMode = VALUESET_EXPANSION_MODE.AUTO;
    private VALUESET_MEMBERSHIP_MODE valuesetMembershipMode = VALUESET_MEMBERSHIP_MODE.AUTO;
    private CODE_LOOKUP_MODE codeLookupMode = CODE_LOOKUP_MODE.AUTO;
    private VALUESET_PRE_EXPANSION_MODE valueSetPreExpansionMode = VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT;

    public VALUESET_EXPANSION_MODE getValuesetExpansionMode() {
        return valuesetExpansionMode;
    }

    public TerminologySettings setValuesetExpansionMode(VALUESET_EXPANSION_MODE valuesetExpansionMode) {
        this.valuesetExpansionMode = valuesetExpansionMode;
        return this;
    }

    public VALUESET_MEMBERSHIP_MODE getValuesetMembershipMode() {
        return valuesetMembershipMode;
    }

    public TerminologySettings setValuesetMembershipMode(VALUESET_MEMBERSHIP_MODE valuesetMembershipMode) {
        this.valuesetMembershipMode = valuesetMembershipMode;
        return this;
    }

    public CODE_LOOKUP_MODE getCodeLookupMode() {
        return codeLookupMode;
    }

    public TerminologySettings setCodeLookupMode(CODE_LOOKUP_MODE codeLookupMode) {
        this.codeLookupMode = codeLookupMode;
        return this;
    }

    public VALUESET_PRE_EXPANSION_MODE getValuesetPreExpansionMode() {
        return this.valueSetPreExpansionMode;
    }

    public TerminologySettings setValuesetPreExpansionMode(VALUESET_PRE_EXPANSION_MODE valuesetPreExpansionMode) {
        this.valueSetPreExpansionMode = valuesetPreExpansionMode;
        return this;
    }
}
