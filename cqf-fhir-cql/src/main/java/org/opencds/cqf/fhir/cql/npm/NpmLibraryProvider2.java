package org.opencds.cqf.fhir.cql.npm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.utilities.npm.NpmPackage;

// LUKETODO:  javadoc
public class NpmLibraryProvider2 implements LibrarySourceProvider {
    private static final String TEXT_CQL = "text/cql";

    private final NpmResourceHolderGetter npmResourceHolderGetter;
    private final NpmResourceHolder npmResourceHolder;

    public NpmLibraryProvider2(NpmResourceHolderGetter npmResourceHolderGetter, NpmResourceHolder npmResourceHolder) {
        this.npmResourceHolderGetter = npmResourceHolderGetter;
        this.npmResourceHolder = npmResourceHolder;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        final String url = toUrl(versionedIdentifier);

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

        final String url = toUrl(versionedIdentifier);

        final Optional<Library> optMainLibrary = npmResourceHolder.getOptMainLibrary();

        if (doesLibraryMatch(versionedIdentifier, optMainLibrary.orElse(null))) {
            return optMainLibrary;
        }

        return npmResourceHolderGetter.loadLibrary(url);
    }

    private static boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier, Library libraryCandidate) {
        return LibraryMatcher.doesLibraryMatch(versionedIdentifier.getId(), libraryCandidate);
    }

    // LUKETODO:  this is not correct:
    private String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // LUKETODO:  get url info from NpmPackages.


        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
    }
}
