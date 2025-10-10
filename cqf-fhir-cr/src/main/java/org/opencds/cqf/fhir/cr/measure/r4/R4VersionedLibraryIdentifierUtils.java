package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.Canonicals.CanonicalParts;

/**
 * Maintain logic for working with {@link VersionedIdentifier} from {@link Library}
 */
public class R4VersionedLibraryIdentifierUtils {

    private R4VersionedLibraryIdentifierUtils() {
        // static class
    }

    static void validateLibraryVersionedIdentifierAndUrl(Bundle bundleWithLibrary, String measureLibraryUrl) {
        final CanonicalParts measureLibraryUrlCanonicalParts = Canonicals.getParts(measureLibraryUrl);

        final Library libraryFromQuery = validateBundle(measureLibraryUrlCanonicalParts, bundleWithLibrary.getEntry());

        final String libraryUrlFromLibrary = libraryFromQuery.getUrl();
        final CanonicalParts libraryUrlFromLibraryCanonicalParts = Canonicals.getParts(libraryUrlFromLibrary);

        validateMatchingLibraryUrls(
                measureLibraryUrlCanonicalParts, libraryUrlFromLibraryCanonicalParts, libraryFromQuery.getVersion());

        //        validateLibraryNameVsUrl(libraryUrlFromLibraryCanonicalParts.url(), libraryFromQuery);
        validateLibraryNameVsUrl(libraryUrlFromLibraryCanonicalParts, libraryFromQuery);
    }

    @Nonnull
    private static Library validateBundle(
            CanonicalParts measureLibraryUrlCanonicalParts, List<BundleEntryComponent> bundleEntries) {
        final String measureLibraryUrl = measureLibraryUrlCanonicalParts.url();

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
    private static void validateMatchingLibraryUrls(
            CanonicalParts measureLibraryUrlCanonicalParts,
            CanonicalParts urlFromLibraryCanonicalParts,
            String versionFromLibrary) {

        if (!measureLibraryUrlCanonicalParts.url().equals(urlFromLibraryCanonicalParts.url())) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because its URL: %s does not match the measure's library URL: %s, which is version-less"
                            .formatted(measureLibraryUrlCanonicalParts.url(), urlFromLibraryCanonicalParts.url()));
        }

        final String versionFromMeasureLibraryUrl = measureLibraryUrlCanonicalParts.version();

        if (versionFromMeasureLibraryUrl != null && !versionFromMeasureLibraryUrl.equals(versionFromLibrary)) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because its URL: %s and version: %s do not match the measure's library URL: %s and version: %s"
                            .formatted(
                                    urlFromLibraryCanonicalParts.url(),
                                    versionFromLibrary,
                                    measureLibraryUrlCanonicalParts.url(),
                                    measureLibraryUrlCanonicalParts.version()));
        }
    }

    private static void validateLibraryNameVsUrl(
            CanonicalParts urlFromLibraryCanonicalParts, Library libraryFromQuery) {

        if (!urlFromLibraryCanonicalParts.tail().equals(libraryFromQuery.getName())) {
            throw new InvalidRequestException(
                    "Library cannot be resolved because the name: %s does not match the version-less last part of the URL: %s"
                            .formatted(libraryFromQuery.getName(), libraryFromQuery.getUrl()));
        }
    }
}
