package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;

// LUKETODO:  javadoc
public class NpmLibraryProvider implements LibrarySourceProvider {
    private static final String TEXT_CQL = "text/cql";

    @Nullable
    private final Library library;

    public NpmLibraryProvider(@Nullable Library library) {
        this.library = library;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {
        if (null == library) {
            return null;
        }

        if (!doesLibraryMatch(versionedIdentifier, library)) {
            return null;
        }

        final List<Attachment> content = library.getContent();

        final Optional<Attachment> optCqlData = content.stream()
            .filter(c -> c.getContentType().equals(TEXT_CQL))
            .findFirst();

        if (optCqlData.isEmpty()) {
            return null;
        }

        final Attachment attachment = optCqlData.get();

        return new ByteArrayInputStream(attachment.getData());
    }

    private static boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier, Library libraryCandidate) {
        return LibraryMatcher.doesLibraryMatch(versionedIdentifier.getId(), libraryCandidate);
    }
}
