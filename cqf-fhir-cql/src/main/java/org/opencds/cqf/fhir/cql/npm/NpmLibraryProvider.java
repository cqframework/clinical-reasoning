package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.fhir.npm.ILibraryReader;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  javadoc
public class NpmLibraryProvider implements LibrarySourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(NpmLibraryProvider.class);

    private static final String TEXT_CQL = "text/cql";

    private final NpmResourceHolder npmResourceHolder;
    private final NpmResourceHolderGetter npmResourceHolderGetter;

    public NpmLibraryProvider(NpmResourceHolderGetter npmResourceHolderGetter, NpmResourceHolder npmResourceHolder) {
        this.npmResourceHolder = npmResourceHolder;
        this.npmResourceHolderGetter = npmResourceHolderGetter;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        final Optional<Library> optLibrary = findLibrary(versionedIdentifier);

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

    private Optional<Library> findLibrary(VersionedIdentifier versionedIdentifier) {

        final String url = npmResourceHolder.toUrl(versionedIdentifier);

        logger.info("******************************");
        logger.info("******************************");
        logger.info("******************************");
        logger.info("versionedIdentifier: {}, resolved URL:{}", versionedIdentifier, url);
        logger.info("******************************");
        logger.info("******************************");
        logger.info("******************************");

        if (npmResourceHolder.doesLibraryMatch(versionedIdentifier)) {
            return npmResourceHolder.getOptMainLibrary();
        }

        return npmResourceHolderGetter.loadLibrary(url);
    }

    // LUKETODO:  this is the secret sauce:  how do we effectively transform an identifier into a URL that we can use to
    // load
    // LUKETODO:  how do we get http://example.com/Library/simple-alpha ?????
    // This doesn't work:  we need the domain name and we can't get that from the versioned identifier
    private String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // LUKETODO:  get url info from NpmPackages.

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        //        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to convert
        final List<NamespaceInfo> namespaceInfos = npmResourceHolder.getNamespaceInfos();

        // LUKETODO:  what if we have more than one?

        final NamespaceInfo namespaceInfo = namespaceInfos.get(0);

        final String uri = namespaceInfo.getUri();
        final String name = namespaceInfo.getName();
        return uri + "/Library/" + versionedIdentifier.getId();
    }

    private static InputStream doSomethingWithNpmAndVersionedIdentifier(
            VersionedIdentifier versionedIdentifier, NpmPackage npmPackage, ILibraryReader libraryReader)
            throws IOException {

        // Massage the versioned identifier using the NpmPackage
        final VersionedIdentifier libraryIdentifier = deriveLibraryIdentifier(versionedIdentifier, npmPackage);

        // Get the package file as an input stream
        final InputStream packageAsInputStream = npmPackage.loadByCanonicalVersion(
                "%s/Library/%s".formatted(libraryIdentifier.getSystem(), libraryIdentifier.getId()),
                libraryIdentifier.getVersion());

        if (packageAsInputStream != null) {
            // For some reason, we load the Library as R5, not R4
            org.hl7.fhir.r5.model.Library l = libraryReader.readLibrary(packageAsInputStream);

            for (org.hl7.fhir.r5.model.Attachment attachment : l.getContent()) {
                if (attachment.getContentType() != null && TEXT_CQL.equals(attachment.getContentType())) {
                    if (versionedIdentifier.getSystem() == null) {
                        versionedIdentifier.setSystem(libraryIdentifier.getSystem());
                    }

                    return new ByteArrayInputStream(attachment.getData());
                }
            }
        }
        return null;
    }

    @Nonnull
    private static VersionedIdentifier deriveLibraryIdentifier(
            VersionedIdentifier versionedIdentifier, NpmPackage npmPackage) {
        VersionedIdentifier libraryIdentifier = new VersionedIdentifier()
                .withId(versionedIdentifier.getId())
                .withVersion(versionedIdentifier.getVersion())
                .withSystem(versionedIdentifier.getSystem());

        if (libraryIdentifier.getSystem() == null) {
            libraryIdentifier.setSystem(npmPackage.canonical());
        }
        return libraryIdentifier;
    }
}
