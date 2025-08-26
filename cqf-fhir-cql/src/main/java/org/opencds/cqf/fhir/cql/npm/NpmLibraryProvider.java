package org.opencds.cqf.fhir.cql.npm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LibrarySourceProvider} to provide a CQL Library Stream from an NPM package.
 */
public class NpmLibraryProvider implements LibrarySourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(NpmLibraryProvider.class);

    private static final String TEXT_CQL = "text/cql";

    private final NpmPackageLoader npmPackageLoader;

    public NpmLibraryProvider(NpmPackageLoader npmPackageLoader) {
        this.npmPackageLoader = npmPackageLoader;
    }

    @Override
    @Nullable
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        var libraryInputStream = npmPackageLoader
                .findMatchingLibrary(versionedIdentifier)
                .map(this::findCqlAttachment)
                .flatMap(Function.identity())
                .map(IAttachmentAdapter::getData)
                .map(ByteArrayInputStream::new)
                .orElse(null);

        if (NpmPackageLoader.DEFAULT != npmPackageLoader) {
            logger.warn(
                    "ATTENTION!  Non-NOOP NPM loader: Could not find CQL Library for identifier: {}",
                    versionedIdentifier);
        }

        return libraryInputStream;
    }

    @Nonnull
    private Optional<IAttachmentAdapter> findCqlAttachment(ILibraryAdapter library) {
        final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(
                library.fhirContext().getVersion().getVersion());

        return library.getContent().stream()
                .map(adapterFactory::createAttachment)
                .filter(attachment -> TEXT_CQL.equals(attachment.getContentType()))
                .findFirst();
    }
}
