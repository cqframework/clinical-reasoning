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

// LUKETODO:  find a proper home for this later
// LUKETODO:  javadoc
// LUKETODO:  rename
// LUKETODO: do the NpmPackages live on the local filesytems so will this work on a cluster?
// LUKETODO:  do we want to bother with other FHIR versions?  If so, how to design for this?
public class R4NpmResourceHolder {
    private static final Logger logger = LoggerFactory.getLogger(R4NpmResourceHolder.class);

    public static final R4NpmResourceHolder EMPTY = new R4NpmResourceHolder(null, null, List.of());

    @Nullable
    private final Measure measure;

    @Nullable
    private final Library mainLibrary;

    private final List<NpmPackage> npmPackages;

    public R4NpmResourceHolder(@Nullable Measure measure, @Nullable Library mainLibrary, List<NpmPackage> npmPackages) {
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
        logger.info("1234: Find matching library for " + versionedIdentifier);

        final Optional<Library> mainLibrary = getOptMainLibrary();
        final Optional<Library> derivedLibrary = loadNpmLibrary(versionedIdentifier);

        if (doesLibraryMatch(versionedIdentifier)) {
            return mainLibrary;
        }

        return derivedLibrary;
    }

    public Optional<Library> findMatchingLibrary(ModelIdentifier modelIdentifier) {
        logger.info("1234: Find matching library for " + modelIdentifier);

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
                .map(R4NpmResourceHolder::convertInputStreamToLibrary)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<Library> loadNpmLibrary(ModelIdentifier modelIdentifier) {
        return npmPackages.stream()
                .map(npmPackage -> loadLibraryAsInputStream(npmPackage, modelIdentifier))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(R4NpmResourceHolder::convertInputStreamToLibrary)
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

    public boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier) {
        return doesLibraryMatch(versionedIdentifier.getId());
    }

    public boolean doesLibraryMatch(ModelIdentifier modelIdentifier) {
        return doesLibraryMatch(modelIdentifier.getId());
    }

    // LUKETODO:  this is the secret sauce:  how do we effectively transform an identifier into a URL that we can use to
    // load
    // LUKETODO:  how do we get http://example.com/Library/simple-alpha ?????
    // This doesn't work:  we need the domain name and we can't get that from the versioned identifier
    public String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // LUKETODO:  get url info from NpmPackages.

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        //        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to convert
        final List<NamespaceInfo> namespaceInfos = getNamespaceInfos();

        // LUKETODO:  what if we have more than one?

        final NamespaceInfo namespaceInfo = namespaceInfos.get(0);

        final String uri = namespaceInfo.getUri();
        final String name = namespaceInfo.getName();
        return uri + "/Library/" + versionedIdentifier.getId();
    }

    public List<NamespaceInfo> getNamespaceInfos() {
        return npmPackages.stream().map(this::getNamespaceInfo).toList();
    }

    @Nonnull
    private NamespaceInfo getNamespaceInfo(NpmPackage npmPackage) {
        // LUKETODO:  do we get a
        return new NamespaceInfo(npmPackage.name(), npmPackage.canonical());
    }

    private static final String TEXT_CQL = "text/cql";

    boolean doesLibraryMatch(String id) {
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

    // LUKETODO:  do we?
    // 1) Do the magic below with the NpmPackage class?
    // 2) Pass in the NpmResourceHolderGetter instead and call loadLibraryByUrl?
    // LUKETODO:  in order to get an ILibraryReader, we need to know the FHIR version, which we can get for a given
    // NpmPackage
    //    private static InputStream doSomethingWithNpmAndVersionedIdentifier(
    //            VersionedIdentifier versionedIdentifier, NpmPackage npmPackage, ILibraryReader libraryReader)
    //        throws IOException {
    //
    //        // Massage the versioned identifier using the NpmPackage
    //        final VersionedIdentifier libraryIdentifier = deriveLibraryIdentifier(versionedIdentifier, npmPackage);
    //
    //        // Get the package file as an input stream
    //        final Optional<InputStream> packageAsInputStream = loadLibraryAsInputStream(npmPackage,
    //            libraryIdentifier);
    //
    //        if (packageAsInputStream.isPresent()) {
    //            final InputStream stream = packageAsInputStream.get();
    //            // For some reason, we load the Library as R5, not R4
    //            org.hl7.fhir.r5.model.Library l = libraryReader.readLibrary(stream);
    //
    //            for (org.hl7.fhir.r5.model.Attachment attachment : l.getContent()) {
    //                if (attachment.getContentType() != null && TEXT_CQL.equals(attachment.getContentType())) {
    //                    // LUKETODO: why did they do this?  why do they to mutate the thing?????
    //                    if (versionedIdentifier.getSystem() == null) {
    //                        versionedIdentifier.setSystem(libraryIdentifier.getSystem());
    //                    }
    //
    //                    return new ByteArrayInputStream(attachment.getData());
    //                }
    //            }
    //        }
    //        return null;
    //    }

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

    @Nonnull
    private static VersionedIdentifier deriveLibraryIdentifier(
            VersionedIdentifier versionedIdentifier, NpmPackage npmPackage) {

        return new VersionedIdentifier()
                .withId(versionedIdentifier.getId())
                .withVersion(versionedIdentifier.getVersion())
                .withSystem(
                        versionedIdentifier.getSystem() == null
                                ? npmPackage.canonical()
                                : versionedIdentifier.getSystem());
    }
}
