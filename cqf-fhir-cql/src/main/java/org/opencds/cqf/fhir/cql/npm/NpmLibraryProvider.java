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
import org.opencds.cqf.fhir.utility.npm.R4NpmResourceInfoForCql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// LUKETODO:  javadoc
public class NpmLibraryProvider implements LibrarySourceProvider {
    private static final Logger logger = LoggerFactory.getLogger(NpmLibraryProvider.class);

    private static final String TEXT_CQL = "text/cql";

    private final R4NpmResourceInfoForCql r4NpmResourceInfoForCql;

    public NpmLibraryProvider(R4NpmResourceInfoForCql r4NpmResourceInfoForCql) {
        this.r4NpmResourceInfoForCql = r4NpmResourceInfoForCql;
    }

    @Override
    @Nullable
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        final Optional<Library> optLibrary = r4NpmResourceInfoForCql.findMatchingLibrary(versionedIdentifier);

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
