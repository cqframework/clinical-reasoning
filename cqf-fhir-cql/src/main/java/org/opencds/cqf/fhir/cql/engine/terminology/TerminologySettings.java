package org.opencds.cqf.fhir.cql.engine.terminology;

/*
 * There are three general terminology operations the the CQL engine performs.
 * 1. Terminology expansion - given a ValueSet url, calculate the set of codes in the value set.
 *      This can be used by CQL authors inline in CQL, but is primarily used to filter resources
 *      like Observations and Conditions that are a member some code set.
 * 2. Terminology filtering - given a value set that represents a set of codes,
 *      filter a set of resources to those that are a member that code set.
 * 3. Terminology membership - give a code, check to see if it's a member of some value set.
 */
public class TerminologySettings {

    // How to do an expansion, if and when we need an expansion.
    public enum EXPANSION_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Repository performs expansion - Force the use of the $expand operation
        REPOSITORY,
        // CQL engine performs expansion - Make a best effort expansion in the CQL engine
        CQL
    }

    // How to do membership, if and when we need membership.
    public enum MEMBERSHIP_MODE {
        // Decreasing order of performance
        // Detect from capability statements
        AUTO,
        // Repository performs membership operation - Force the use of the $validate-code operation
        REPOSITORY,
        // CQL engine performs membership operation - Force the CQL engine to do the membership operation.
        CQL
    }

    private EXPANSION_MODE expansionMode = EXPANSION_MODE.AUTO;
    private MEMBERSHIP_MODE memberShipMode = MEMBERSHIP_MODE.AUTO;

    public EXPANSION_MODE getExpansionMode() {
        return expansionMode;
    }

    public TerminologySettings setExpansionMode(EXPANSION_MODE expansionMode) {
        this.expansionMode = expansionMode;
        return this;
    }

    public MEMBERSHIP_MODE getMemberShipMode() {
        return memberShipMode;
    }

    public TerminologySettings setMemberShipMode(MEMBERSHIP_MODE memberShipMode) {
        this.memberShipMode = memberShipMode;
        return this;
    }
    ;
}
