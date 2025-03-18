package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  javadoc
public class R4NpmResourceInfoForCql {
    private static final Logger logger = LoggerFactory.getLogger(R4NpmResourceInfoForCql.class);

    public static final R4NpmResourceInfoForCql EMPTY = new R4NpmResourceInfoForCql(null, null, List.of());

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    private final List<NpmPackage> npmPackages;

    public R4NpmResourceInfoForCql(@Nullable Measure measure, @Nullable Library mainLibrary, List<NpmPackage> npmPackages) {
        this.measure = measure;
        this.mainLibrary = mainLibrary;
        this.npmPackages = npmPackages;
    }

    public Optional<Measure> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public Optional<Library> getOptMainLibrary() {
        return Optional.ofNullable(mainLibrary);
    }

    public Optional<Library> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {

        final Optional<Library> mainLibrary = getOptMainLibrary();
        final Optional<Library> derivedLibrary = loadNpmLibrary(versionedIdentifier);

        if (doesLibraryMatch(versionedIdentifier)) {
            return mainLibrary;
        }

        return derivedLibrary;
    }

    public Optional<Library> findMatchingLibrary(ModelIdentifier modelIdentifier) {

        final Optional<Library> mainLibrary = getOptMainLibrary();
        final Optional<Library> derivedLibrary = loadNpmLibrary(modelIdentifier);

        if (doesLibraryMatch(modelIdentifier)) {
            return mainLibrary;
        }

        return derivedLibrary;
    }

    private Optional<Library> loadNpmLibrary(VersionedIdentifier versionedIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryAsInputStream(npmPackage, versionedIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(R4NpmResourceInfoForCql::convertInputStreamToLibrary)
                .flatMap(Optional::stream)
                .findFirst();
    }

    public List<NamespaceInfo> getNamespaceInfos() {
        return npmPackages.stream().map(this::getNamespaceInfo).toList();
    }

    @Nonnull
    private NamespaceInfo getNamespaceInfo(NpmPackage npmPackage) {
        // LUKETODO:  verify we want a canonical and not a url here:
        return new NamespaceInfo(npmPackage.name(), npmPackage.canonical());
    }

    private Optional<Library> loadNpmLibrary(ModelIdentifier modelIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryAsInputStream(npmPackage, modelIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(R4NpmResourceInfoForCql::convertInputStreamToLibrary)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static InputStream loadByCanonical(NpmPackage npmPackage, String url) {
        try {
            return npmPackage.loadByCanonical(url);
        } catch (IOException e) {
            throw new InternalErrorException("Could not load npm library: " + url, e);
        }
    }

    private boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier) {
        return doesLibraryMatch(versionedIdentifier.getId());
    }

    private boolean doesLibraryMatch(ModelIdentifier modelIdentifier) {
        return doesLibraryMatch(modelIdentifier.getId());
    }

    private static final String TEXT_CQL = "text/cql";

    private boolean doesLibraryMatch(String id) {
        if (mainLibrary == null) {
            return false;
        }

        if (mainLibrary.getIdPart().equals(id)) {
            final Optional<Attachment> optCqlData = mainLibrary.getContent().stream()
                    .filter(content -> TEXT_CQL.equals(content.getContentType()))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static Optional<Library> convertInputStreamToLibrary(@Nullable InputStream libraryInputStream) {
        try {
            if (libraryInputStream == null) {
                return Optional.empty();
            }

            final Resource resource = new JsonParser().parse(libraryInputStream);
            if (resource instanceof Library library) {
                return Optional.of(library);
            }

            return Optional.empty();
        } catch (Exception exception) {
            throw new InternalErrorException("Failed to load library as input stream", exception);
        }
    }

    private static Optional<InputStream> loadLibraryAsInputStream(
            NpmPackage npmPackage, VersionedIdentifier libraryIdentifier) {

        try {
            return Optional.ofNullable(
                    // LUKETODO:  figure out how to set up version in tgz
                    //                    npmPackage.loadByCanonicalVersion(buildUrl(npmPackage, libraryIdentifier),
                    // libraryIdentifier.getVersion()));
                    npmPackage.loadByCanonical(buildUrl(npmPackage, libraryIdentifier)));
        } catch (IOException exception) {
            throw new InternalErrorException("Failed to load NPM package: " + libraryIdentifier.getId(), exception);
        }
    }

    private static Optional<InputStream> loadLibraryAsInputStream(
            NpmPackage npmPackage, ModelIdentifier modelIdentifier) {

        try {
            return Optional.ofNullable(
                    // LUKETODO:  figure out how to set up version in tgz
                    //                    npmPackage.loadByCanonicalVersion(buildUrl(npmPackage, modelIdentifier),
                    // modelIdentifier.getVersion()));
                    npmPackage.loadByCanonical(buildUrl(npmPackage, modelIdentifier)));
        } catch (IOException exception) {
            throw new InternalErrorException("Failed to load NPM package: " + modelIdentifier.getId(), exception);
        }
    }

    // LUKETODO:  which is it?  url()?  canonical?  system()?
    @Nonnull
    private static String buildUrl(NpmPackage npmPackage, VersionedIdentifier libraryIdentifier) {
        //        return "%s/Library/%s".formatted(npmPackage.url(), libraryIdentifier.getId());
        //        return "%s/Library/%s".formatted(libraryIdentifier.getSystem(), libraryIdentifier.getId());
        // LUKETODO: fudge it for now
        return "%s/Library/%s".formatted("http://example.com", libraryIdentifier.getId());
    }

    @Nonnull
    private static String buildUrl(NpmPackage npmPackage, ModelIdentifier modelIdentifier) {
        //        return "%s/Library/%s-ModelInfo".formatted(npmPackage.url(),modelIdentifier.getId());
        // LUKETODO: fudge it for now
        return "%s/Library/%s-ModelInfo".formatted("http://example.com", modelIdentifier.getId());
    }
}
