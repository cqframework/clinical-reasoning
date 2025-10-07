package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.cql.VersionedIdentifiers;

/**
 * Maintain logic for building and validating {@link VersionedIdentifier} from {@link Library}
 */
public class R4VersionedLibraryIdentifierUtils {

    private static final Pattern LIBRARY_REGEX = Pattern.compile("/Library/");

    private R4VersionedLibraryIdentifierUtils() {
        // static class
    }

    static VersionedIdentifier buildLibraryVersionedIdentifier(Bundle bundleWithLibrary, String measureLibraryUrl) {
        final List<BundleEntryComponent> bundleEntries = bundleWithLibrary.getEntry();
        if (bundleEntries.isEmpty()) {
            var errorMsg = "Unable to find Library with url: %s".formatted(measureLibraryUrl);

            throw new ResourceNotFoundException(errorMsg);
        }

        if (bundleEntries.get(0).getResource() instanceof Library libraryFromQuery) {
            final VersionedIdentifier versionedIdentifierFromMeasureLibraryUrl =
                    VersionedIdentifiers.forUrl(measureLibraryUrl);

            if (doesVersionedIdentifierMatchLibrary(versionedIdentifierFromMeasureLibraryUrl, libraryFromQuery)) {
                return versionedIdentifierFromMeasureLibraryUrl;
            }

            return buildLibraryVersionedIdentifier(libraryFromQuery);
        }

        throw new InternalErrorException("Resource with url: %s is not a Library".formatted(measureLibraryUrl));
    }

    private static boolean doesVersionedIdentifierMatchLibrary(
            VersionedIdentifier versionedIdentifier, Library library) {

        if (!versionedIdentifier.getId().equals(library.getName())) {
            return false;
        }

        if (!doSystemsMatch(versionedIdentifier, library)) {
            return false;
        }

        // If no version is specified in the VersionedIdentifier, we consider it a match
        if (versionedIdentifier.getVersion() == null
                || versionedIdentifier.getVersion().isEmpty()) {
            return true;
        }

        return versionedIdentifier.getVersion().equals(library.getVersion());
    }

    private static boolean doSystemsMatch(VersionedIdentifier versionedIdentifier, Library library) {
        return getSystemFromLibrary(library) == null && versionedIdentifier.getSystem() == null;
    }

    private static VersionedIdentifier buildLibraryVersionedIdentifier(Library library) {
        return new VersionedIdentifier()
                .withId(library.getName())
                .withSystem(getSystemFromLibrary(library))
                .withVersion(library.getVersion());
    }

    @Nullable
    private static String getSystemFromLibrary(Library library) {
        final String urlFromLibrary = library.getUrl();
        if (urlFromLibrary == null || urlFromLibrary.isEmpty()) {
            return null;
        }
        final String[] libraryUrlSplit = LIBRARY_REGEX.split(urlFromLibrary);
        return libraryUrlSplit[0];
    }
}
