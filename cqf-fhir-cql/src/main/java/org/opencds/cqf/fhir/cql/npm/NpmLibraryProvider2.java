package org.opencds.cqf.fhir.cql.npm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;

// LUKETODO:  javadoc
public class NpmLibraryProvider2 implements LibrarySourceProvider {
    private static final String TEXT_CQL = "text/cql";

    private final NpmResourceHolderGetter npmResourceHolderGetter;

    public NpmLibraryProvider2(NpmResourceHolderGetter npmResourceHolderGetter) {
        this.npmResourceHolderGetter = npmResourceHolderGetter;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        final String url = toUrl(versionedIdentifier);

        final Optional<Library> optLoadedLibrary = npmResourceHolderGetter.loadLibrary(url);

        final Optional<Attachment> optCqlData = optLoadedLibrary.map(Library::getContent)
            .stream()
            .flatMap(Collection::stream)
            .filter(content -> content.getContentType().equals(TEXT_CQL))
            .findFirst();

        if (optCqlData.isEmpty()) {
            return null;
        }

        final Attachment attachment = optCqlData.get();

        return new ByteArrayInputStream(attachment.getData());
    }

    private static String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert system to URL

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
    }
}
