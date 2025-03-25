package org.opencds.cqf.fhir.utility.npm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;

public class NpmResourceInfoForCql {

    public static final NpmResourceInfoForCql EMPTY = new NpmResourceInfoForCql(null, null, List.of());

    private static final String TEXT_CQL = "text/cql";

    @Nullable
    private final IMeasureAdapter measure;

    @Nullable
    private final ILibraryAdapter mainLibrary;

    // In theory, it's possible to have more than one associated NpmPackage
    private final List<NpmPackage> npmPackages;

    @Nullable
    private final IAdapterFactory adapterFactory;

    public NpmResourceInfoForCql(
            @Nullable IMeasureAdapter measure, @Nullable ILibraryAdapter mainLibrary, List<NpmPackage> npmPackages) {
        this.measure = measure;
        this.mainLibrary = mainLibrary;
        this.npmPackages = npmPackages;

        adapterFactory = Optional.ofNullable(measure)
                .map(measureNonNull -> IAdapterFactory.forFhirVersion(
                        measureNonNull.fhirContext().getVersion().getVersion()))
                .orElse(null);
    }

    public Optional<IMeasureAdapter> getMeasure() {
        return Optional.ofNullable(measure);
    }

    public Optional<ILibraryAdapter> getOptMainLibrary() {
        return Optional.ofNullable(mainLibrary);
    }

    @VisibleForTesting
    List<NpmPackage> getNpmPackages() {
        return npmPackages;
    }

    public Optional<ILibraryAdapter> findMatchingLibrary(VersionedIdentifier versionedIdentifier) {

        final Optional<ILibraryAdapter> optMainLibrary = getOptMainLibrary();

        if (doesLibraryMatch(versionedIdentifier)) {
            return optMainLibrary;
        }

        final Optional<ILibraryAdapter> optDerivedLibrary = loadNpmLibrary(versionedIdentifier);

        return optDerivedLibrary;
    }

    public Optional<ILibraryAdapter> findMatchingLibrary(ModelIdentifier modelIdentifier) {

        final Optional<ILibraryAdapter> optMainLibrary = getOptMainLibrary();

        if (doesLibraryMatch(modelIdentifier)) {
            return optMainLibrary;
        }

        final Optional<ILibraryAdapter> optDerivedLibrary = loadNpmLibrary(modelIdentifier);

        return optDerivedLibrary;
    }

    private Optional<ILibraryAdapter> loadNpmLibrary(VersionedIdentifier versionedIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryInputStreamContext(npmPackage, versionedIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::convertContextToLibrary)
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
    private Optional<ILibraryAdapter> loadNpmLibrary(ModelIdentifier modelIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryInputStreamContext(npmPackage, modelIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::convertContextToLibrary)
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
        if (mainLibrary == null || adapterFactory == null) {
            return false;
        }

        if (mainLibrary.getId().getIdPart().equals(id)) {
            return mainLibrary.getContent().stream()
                    .map(adapterFactory::createAttachment)
                    .anyMatch(attachment -> TEXT_CQL.equals(attachment.getContentType()));
        }

        return false;
    }

    record LibraryInputStreamContext(FhirVersionEnum fhirVersionEnum, InputStream libraryInputStream) {}

    Optional<LibraryInputStreamContext> loadLibraryInputStreamContext(
            NpmPackage npmPackage, VersionedIdentifier libraryIdentifier) {
        return loadLibraryAsInputStream(npmPackage, libraryIdentifier)
                .map(inputStream -> new LibraryInputStreamContext(
                        FhirVersionEnum.forVersionString(npmPackage.fhirVersion()), inputStream));
    }

    Optional<LibraryInputStreamContext> loadLibraryInputStreamContext(
            NpmPackage npmPackage, ModelIdentifier modelIdentifier) {
        return loadLibraryAsInputStream(npmPackage, modelIdentifier)
                .map(inputStream -> new LibraryInputStreamContext(
                        FhirVersionEnum.forVersionString(npmPackage.fhirVersion()), inputStream));
    }

    private Optional<ILibraryAdapter> convertContextToLibrary(LibraryInputStreamContext context) {
        try {
            final IBaseResource resource = FhirContext.forCached(context.fhirVersionEnum)
                    .newJsonParser()
                    .parseResource(context.libraryInputStream);

            if (adapterFactory != null) {
                return Optional.of(adapterFactory.createLibrary(resource));
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
