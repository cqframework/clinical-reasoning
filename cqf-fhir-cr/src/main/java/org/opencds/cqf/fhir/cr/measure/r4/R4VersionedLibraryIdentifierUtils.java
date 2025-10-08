package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Library;

/**
 * Maintain logic for working with {@link VersionedIdentifier} from {@link Library}
 */
public class R4VersionedLibraryIdentifierUtils {

    private static final Pattern LIBRARY_REGEX = Pattern.compile("/Library/");
    private static final Pattern PIPE_REGEX = Pattern.compile("\\|");

    private R4VersionedLibraryIdentifierUtils() {
        // static class
    }

    static void validateLibraryVersionedIdentifierAndUrl(Bundle bundleWithLibrary, String measureLibraryUrl) {
        final List<BundleEntryComponent> bundleEntries = bundleWithLibrary.getEntry();

        final Library libraryFromQuery = validateBundle(measureLibraryUrl, bundleEntries);

        final String libraryUrlFromLibrary = libraryFromQuery.getUrl();

        validateMatchingLibraryUrls(measureLibraryUrl, libraryUrlFromLibrary);

        validateLibraryNameVsUrl(libraryUrlFromLibrary, libraryFromQuery);
    }

    @Nonnull
    private static Library validateBundle(String measureLibraryUrl, List<BundleEntryComponent> bundleEntries) {

        if (bundleEntries.isEmpty()) {
            var errorMsg = "Unable to find Library with url: %s".formatted(measureLibraryUrl);

            throw new ResourceNotFoundException(errorMsg);
        }

        if (!(bundleEntries.get(0).getResource() instanceof Library libraryFromQuery)) {
            throw new InternalErrorException("Resource with url: %s is not a Library".formatted(measureLibraryUrl));
        }

        return libraryFromQuery;
    }

    // A measure library URL may be versioned with a pipe, but the URL from the library will not
    // have a piped version, only a separate version field.
    private static void validateMatchingLibraryUrls(String measureLibraryUrl, String urlFromLibrary) {

        final String[] measureLibraryUrlVersionSplit = PIPE_REGEX.split(measureLibraryUrl);

        // Strip off version if it exists
        final String measureLibraryUrlWithoutVersion =
                measureLibraryUrlVersionSplit.length == 2 ? measureLibraryUrlVersionSplit[0] : measureLibraryUrl;

        if (!measureLibraryUrlWithoutVersion.equals(urlFromLibrary)) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because its URL: %s does not match the measure's library URL: %s"
                            .formatted(urlFromLibrary, measureLibraryUrl));
        }
    }

    private static void validateLibraryNameVsUrl(String urlFromLibrary, Library libraryFromQuery) {

        final String[] libraryUrlSplit = LIBRARY_REGEX.split(urlFromLibrary);

        if (libraryUrlSplit.length != 2) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because its URL: %s is not valid".formatted(urlFromLibrary));
        }

        final String libraryNameAndVersionFromUrl = libraryUrlSplit[1];
        final String[] nameVersionSplit = PIPE_REGEX.split(libraryNameAndVersionFromUrl);

        // Strip off version if it exists
        final String libraryNameFromUrl =
                nameVersionSplit.length == 2 ? nameVersionSplit[0] : libraryNameAndVersionFromUrl;

        if (!libraryNameFromUrl.equals(libraryFromQuery.getName())) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because the name: %s does not match the version-less last part of the URL: %s"
                            .formatted(libraryFromQuery.getName(), urlFromLibrary));
        }
    }
}
