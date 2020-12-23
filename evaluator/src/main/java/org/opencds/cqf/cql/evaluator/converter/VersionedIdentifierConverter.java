package org.opencds.cqf.cql.evaluator.converter;

import org.cqframework.cql.elm.execution.VersionedIdentifier;

/**
 * This class converts between the the translator and engine VersionedIdentifier types.
 */
public class VersionedIdentifierConverter {

    /**
     * Converts an org.cqframework.cql.elm.execution.VersionedIdentifier to an org.hl7.elm.r1.VersionedIdentifier.
     * 
     * Returns null if versionedIdentifier is null.
     * @param versionedIdentifier the engine versionedIdentifier
     * @return the equivalent translator versionedIdentifier
     */
    public static org.hl7.elm.r1.VersionedIdentifier toElmIdentifier(VersionedIdentifier versionedIdentifier) {
        if (versionedIdentifier == null) {
            return null;
        }

        return new org.hl7.elm.r1.VersionedIdentifier()
        .withSystem(versionedIdentifier.getSystem())
        .withId(versionedIdentifier.getId())
        .withVersion(versionedIdentifier.getVersion());
    }

    /**
     * Converts an org.hl7.elm.r1.VersionedIdentifier to an org.cqframework.cql.elm.execution.VersionedIdentifier. 
     * 
     * Returns null if versionedIdentifier is null.
     * @param versionedIdentifier the translator versionedIdentifier
     * @return the equivalent engine versionedIdentifier
     */
    public static VersionedIdentifier toEngineIdentifier(org.hl7.elm.r1.VersionedIdentifier versionedIdentifier) {
        if (versionedIdentifier == null) {
            return null;
        }

        return new VersionedIdentifier()
        .withSystem(versionedIdentifier.getSystem())
        .withId(versionedIdentifier.getId())
        .withVersion(versionedIdentifier.getVersion());
    }
}
