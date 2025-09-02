package org.opencds.cqf.fhir.cql;

import java.util.regex.Pattern;
import org.hl7.elm.r1.VersionedIdentifier;

public class VersionedIdentifiers {
    private static final Pattern LIBRARY_SPLIT_PATTERN = Pattern.compile("Library/");

    private VersionedIdentifiers() {
        // empty
    }

    public static VersionedIdentifier forUrl(String url) {
        if (!url.contains("/Library/") && !url.startsWith("Library/")) {
            throw new IllegalArgumentException(
                    "Invalid resource type for determining library version identifier: Library");
        }

        final String[] urlSplitByLibrary = LIBRARY_SPLIT_PATTERN.split(url);
        if (urlSplitByLibrary.length > 2) {
            throw new IllegalArgumentException(
                    "Invalid url, Library.url SHALL be <CQL namespace url>/Library/<CQL library name>");
        }

        final String cqlName = urlSplitByLibrary.length == 1 ? urlSplitByLibrary[0] : urlSplitByLibrary[1];
        VersionedIdentifier versionedIdentifier = new VersionedIdentifier();
        if (cqlName.contains("|")) {
            String[] nameVersion = cqlName.split("\\|");
            String name = nameVersion[0];
            String version = nameVersion[1];
            versionedIdentifier.setId(name);
            versionedIdentifier.setVersion(version);
        } else {
            versionedIdentifier.setId(cqlName);
        }

        return versionedIdentifier;
    }
}
