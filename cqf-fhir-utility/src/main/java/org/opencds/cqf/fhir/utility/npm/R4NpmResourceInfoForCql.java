package org.opencds.cqf.fhir.utility.npm;

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

/**
 * Container for {@link Measure} and {@link Library}, and a List of associated {@link NpmPackage}s.
 * Encapsulate the NpmPackages by only exposing the {@link NamespaceInfo}s and derived
 * {@link Library}s not directly associated with a given Measure.
 */
public class R4NpmResourceInfoForCql {

    public static final R4NpmResourceInfoForCql EMPTY = new R4NpmResourceInfoForCql(null, null, List.of());

    private static final String TEXT_CQL = "text/cql";

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    // In theory, it's possible to have more than one associated NpmPackage
    private final List<NpmPackage> npmPackages;

    public R4NpmResourceInfoForCql(
            @Nullable Measure measure, @Nullable Library mainLibrary, List<NpmPackage> npmPackages) {
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

        final Optional<Library> optMainLibrary = getOptMainLibrary();
        final Optional<Library> optDerivedLibrary = loadNpmLibrary(versionedIdentifier);

        if (doesLibraryMatch(versionedIdentifier)) {
            return optMainLibrary;
        }

        return optDerivedLibrary;
    }

    public Optional<Library> findMatchingLibrary(ModelIdentifier modelIdentifier) {

        final Optional<Library> optMainLibrary = getOptMainLibrary();
        final Optional<Library> optDerivedLibrary = loadNpmLibrary(modelIdentifier);

        if (doesLibraryMatch(modelIdentifier)) {
            return optMainLibrary;
        }

        return optDerivedLibrary;
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
        return new NamespaceInfo(npmPackage.name(), npmPackage.canonical());
    }

    // Note that this code hasn't actually been tested and is not needed at the present time.
    // If this should change, the code will need to be tested and possibly modified.
    private Optional<Library> loadNpmLibrary(ModelIdentifier modelIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryAsInputStream(npmPackage, modelIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(R4NpmResourceInfoForCql::convertInputStreamToLibrary)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier) {
        return doesLibraryMatch(versionedIdentifier.getId());
    }

    private boolean doesLibraryMatch(ModelIdentifier modelIdentifier) {
        return doesLibraryMatch(modelIdentifier.getId());
    }

    private boolean doesLibraryMatch(String id) {
        if (mainLibrary == null) {
            return false;
        }

        if (mainLibrary.getIdPart().equals(id)) {
            final Optional<Attachment> optCqlData = mainLibrary.getContent().stream()
                    .filter(content -> TEXT_CQL.equals(content.getContentType()))
                    .findFirst();

            return optCqlData.isPresent();
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
            return Optional.ofNullable(npmPackage.loadByCanonicalVersion(
                    buildUrl(npmPackage, libraryIdentifier), libraryIdentifier.getVersion()));
        } catch (IOException exception) {
            throw new InternalErrorException("Failed to load NPM package: " + libraryIdentifier.getId(), exception);
        }
    }

    private static Optional<InputStream> loadLibraryAsInputStream(
            NpmPackage npmPackage, ModelIdentifier modelIdentifier) {

        try {
            return Optional.ofNullable(npmPackage.loadByCanonicalVersion(
                    buildUrl(npmPackage, modelIdentifier), modelIdentifier.getVersion()));
        } catch (IOException exception) {
            throw new InternalErrorException("Failed to load NPM package: " + modelIdentifier.getId(), exception);
        }
    }

    @Nonnull
    private static String buildUrl(NpmPackage npmPackage, VersionedIdentifier libraryIdentifier) {
        return "%s/Library/%s".formatted(npmPackage.canonical(), libraryIdentifier.getId());
    }

    @Nonnull
    private static String buildUrl(NpmPackage npmPackage, ModelIdentifier modelIdentifier) {
        return "%s/Library/%s-ModelInfo".formatted(npmPackage.canonical(), modelIdentifier.getId());
    }
}
