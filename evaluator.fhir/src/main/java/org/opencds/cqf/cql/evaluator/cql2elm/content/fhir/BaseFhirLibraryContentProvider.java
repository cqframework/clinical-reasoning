package org.opencds.cqf.cql.evaluator.cql2elm.content.fhir;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentType;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AttachmentAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.LibraryAdapter;

/**
 * This class implements logic for extracting content from a FHIR Library resource and provides an extension point
 * for implementing the fetch of a FHIR library matching a specific identifier.
 */
public abstract class BaseFhirLibraryContentProvider
        implements LibraryContentProvider {

    protected AdapterFactory adapterFactory;

    protected BaseFhirLibraryContentProvider(AdapterFactory adapterFactory) {
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

        switch(libraryContentType) {
            case CQL: return this.getContentStream(library, "text/cql");
            case XML: return this.getContentStream(library, "application/elm+xml");
            case JXSON: 
            case JSON:
                return this.getContentStream(library, "application/elm+json");
            case COFFEE:
            default:
                throw new UnsupportedOperationException(String.format("This content provider does not support the %s LibraryContentType", libraryContentType.toString()));

        }
    }

    protected InputStream getContentStream(IBaseResource library, String contentType) {

        LibraryAdapter libraryAdapter = this.adapterFactory.createLibrary(library);
        
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

    protected abstract IBaseResource getLibrary(VersionedIdentifier libraryIdentifier);
}