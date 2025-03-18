package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  javadoc
public class NpmLibraryProvider implements LibrarySourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(NpmLibraryProvider.class);

    private static final String TEXT_CQL = "text/cql";

    private final R4NpmResourceHolder r4NpmResourceHolder;
    // LUKETODO:  do we need this anymore?
    private final R4NpmPackageLoader r4NpmPackageLoader;

    public NpmLibraryProvider(R4NpmPackageLoader r4NpmPackageLoader, R4NpmResourceHolder r4NpmResourceHolder) {
        this.r4NpmResourceHolder = r4NpmResourceHolder;
        this.r4NpmPackageLoader = r4NpmPackageLoader;
    }

    // LUKETODO:  steps
    // 1) Check the Library retrieved directly from the results of searching for the Measure and its associated Library
    // URL and see if it matches with the VersionedIdentifier
    // 2) If it doesn't match, then search the NpmPackages associated with the Measure by massaging the
    // VersionedIdentifier into a URL
    // 3) So what do we load?  Only the NPM packages associated with the Measure, or somehow retrieve ALL of them for
    // the CQL engine?

    @Override
    @Nullable
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        final Optional<Library> optLibrary = r4NpmResourceHolder.findMatchingLibrary(versionedIdentifier);

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
}
