package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoader;
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LibrarySourceProvider} to provide a CQL Library Stream from an NPM package.
 */
public class NpmLibraryProvider implements LibrarySourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(NpmLibraryProvider.class);

    private static final String TEXT_CQL = "text/cql";

    private final R4NpmResourceInfoForCql r4NpmResourceInfoForCql;
    private final R4NpmPackageLoader r4NpmPackageLoader;

    public NpmLibraryProvider(R4NpmResourceInfoForCql r4NpmResourceInfoForCql, R4NpmPackageLoader r4NpmPackageLoader) {
        this.r4NpmResourceInfoForCql = r4NpmResourceInfoForCql;
        this.r4NpmPackageLoader = r4NpmPackageLoader;
    }

    @Override
    @Nullable
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        final Optional<Library> optLibrary = findMatchingLibrary(versionedIdentifier);

        final Optional<Attachment> optCqlData = optLibrary.map(Library::getContent).stream()
                .flatMap(Collection::stream)
                .filter(content -> content.getContentType().equals(TEXT_CQL))
                .findFirst();

        if (optCqlData.isEmpty()) {
            return null;
        }

        final Attachment attachment = optCqlData.get();

        return new ByteArrayInputStream(attachment.getData());
    }

    private Optional<Library> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {
        return r4NpmResourceInfoForCql
                .findMatchingLibrary(versionedIdentifier)
                .or(() -> findLibraryFromUnrelatedNpmPackage(versionedIdentifier));
    }

    private Optional<Library> findLibraryFromUnrelatedNpmPackage(VersionedIdentifier versionedIdentifier) {
        return r4NpmPackageLoader.loadLibraryByUrl(getUrl(versionedIdentifier));
    }

    /*
      {
          "name": "org.opencds.npm.with-derived-library",
          "version":"0.1",
          "canonical":"http://with-derived-library.npm.opencds.org",
          "url":"http://with-derived-library.npm.opencds.org/Draft1",
          "author": "luke",
          "fhirVersions": [
          "4.0.1"
    ],
          "dependencies": {
          "hl7.fhir.r4.core": "4.0.1"
      }
      }
       */

    private static final Map<String, String> ID_TO_URL = Map.of("x", "http://with-derived-library.npm.opencds.org");

    private String getUrl(VersionedIdentifier versionedIdentifier) {
        return "%s/Library/%s".formatted(versionedIdentifier.getSystem(), versionedIdentifier.getId());
    }
}
