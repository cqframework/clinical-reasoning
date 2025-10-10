package org.opencds.cqf.fhir.cql;

import java.util.regex.Pattern;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Canonicals.CanonicalParts;

public class VersionedIdentifiers {

    private static final Pattern LIBRARY_REGEX = Pattern.compile("/Library/");

    private VersionedIdentifiers() {
        // empty
    }

    public static VersionedIdentifier forUrl(String libraryUrl) {
        final CanonicalParts libraryUrlCanonicalParts = Canonicals.getParts(libraryUrl);

        if (!libraryUrlCanonicalParts.resourceType().equals("Library")) {
            throw new IllegalArgumentException(
                    "Invalid resource type for determining library version identifier: Library");
        }

        if (LIBRARY_REGEX.split(libraryUrlCanonicalParts.url()).length > 2) {
            throw new IllegalArgumentException(
                    "Invalid libraryUrl, Library.libraryUrl SHALL be <CQL namespace libraryUrl>/Library/<CQL library name>");
        }

        if (libraryUrlCanonicalParts.tail() == null
                || libraryUrlCanonicalParts.tail().isBlank()) {
            throw new IllegalArgumentException("libraryUrl must contain a library name");
        }

        final VersionedIdentifier versionedIdentifier = new VersionedIdentifier();

        if (libraryUrlCanonicalParts.version() != null
                && !libraryUrlCanonicalParts.version().isBlank()) {
            versionedIdentifier.setVersion(libraryUrlCanonicalParts.version());
        }

        versionedIdentifier.setId(libraryUrlCanonicalParts.tail());
        versionedIdentifier.setSystem(libraryUrlCanonicalParts.canonicalBase());

        return versionedIdentifier;
    }
}
