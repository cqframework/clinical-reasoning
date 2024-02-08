package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.cqframework.cql.cql2elm.LibraryContentType;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.AttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;

/**
 * This class implements logic for extracting content from a FHIR Library resource and provides an
 * extension point for implementing the fetch of a FHIR library matching a specific identifier.
 */
public abstract class BaseFhirLibrarySourceProvider implements LibrarySourceProvider {

    protected AdapterFactory adapterFactory;

    protected BaseFhirLibrarySourceProvider(AdapterFactory adapterFactory) {
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    @Override
    public InputStream getLibraryContent(VersionedIdentifier libraryIdentifier, LibraryContentType libraryContentType) {
        requireNonNull(libraryIdentifier, "versionedIdentifier can not be null.");
        requireNonNull(libraryContentType, "libraryContentType can not be null.");

        IBaseResource library = this.getLibrary(libraryIdentifier);
        if (library == null) {
            return null;
        }

        return this.getContentStream(library, libraryContentType.mimeType());
    }

    protected InputStream getContentStream(IBaseResource library, String contentType) {

        IBaseLibraryAdapter libraryAdapter = this.adapterFactory.createLibrary(library);

        if (libraryAdapter.hasContent()) {
            for (ICompositeType attachment : libraryAdapter.getContent()) {
                AttachmentAdapter attachmentAdapter = this.adapterFactory.createAttachment(attachment);
                if (attachmentAdapter.getContentType().equals(contentType)) {
                    return new ByteArrayInputStream(attachmentAdapter.getData());
                }
            }
        }

        return null;
    }

    @Override
    public InputStream getLibrarySource(VersionedIdentifier libraryIdentifier) {
        return getLibraryContent(libraryIdentifier, LibraryContentType.CQL);
    }

    protected abstract IBaseResource getLibrary(VersionedIdentifier libraryIdentifier);
}
