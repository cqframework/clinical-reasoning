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
import org.opencds.cqf.fhir.utility.npm.NpmResourceInfoForCql;

/**
 * {@link LibrarySourceProvider} to provide a CQL Library Stream from an NPM package.
 */
public class NpmLibraryProvider implements LibrarySourceProvider {

    private static final String TEXT_CQL = "text/cql";

    private final NpmResourceInfoForCql npmResourceInfoForCql;
    private final NpmPackageLoader npmPackageLoader;

    public NpmLibraryProvider(NpmResourceInfoForCql npmResourceInfoForCql, NpmPackageLoader npmPackageLoader) {
        this.npmResourceInfoForCql = npmResourceInfoForCql;
        this.npmPackageLoader = npmPackageLoader;
    }

    @Override
    @Nullable
    public InputStream getLibrarySource(VersionedIdentifier versionedIdentifier) {

        return npmPackageLoader
                .findMatchingLibrary(npmResourceInfoForCql, versionedIdentifier)
                .map(this::findCqlAttachment)
                .flatMap(Function.identity())
                .map(IAttachmentAdapter::getData)
                .map(ByteArrayInputStream::new)
                .orElse(null);
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
