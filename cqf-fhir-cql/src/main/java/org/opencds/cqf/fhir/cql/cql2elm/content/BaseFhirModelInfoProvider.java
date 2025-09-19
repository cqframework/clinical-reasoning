package org.opencds.cqf.fhir.cql.cql2elm.content;

import static java.util.Objects.requireNonNull;

import jakarta.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

/**
 * This class implements logic for extracting content from a FHIR Library resource and provides an
 * extension point for implementing the fetch of a FHIR library matching a specific identifier.
 */
public abstract class BaseFhirModelInfoProvider implements ModelInfoProvider {

    protected IAdapterFactory adapterFactory;

    protected BaseFhirModelInfoProvider(IAdapterFactory adapterFactory) {
        this.adapterFactory = requireNonNull(adapterFactory, "adapterFactory can not be null");
    }

    @Override
    public ModelInfo load(ModelIdentifier modelIdentifier) {
        var is = getModelInfoContent(modelIdentifier, ModelInfoContentType.XML);
        return JAXB.unmarshal(is, ModelInfo.class);
    }

    protected InputStream getModelInfoContent(
            ModelIdentifier modelIdentifier, ModelInfoContentType modelInfoContentType) {
        requireNonNull(modelIdentifier, "versionedIdentifier can not be null.");
        requireNonNull(modelInfoContentType, "modelInfoContentType can not be null.");

        IBaseResource library = this.getLibrary(modelIdentifier);
        if (library == null) {
            return null;
        }

        return this.getContentStream(library, modelInfoContentType.mimeType());
    }

    protected InputStream getContentStream(IBaseResource library, String contentType) {

        ILibraryAdapter libraryAdapter = this.adapterFactory.createLibrary(library);

        if (libraryAdapter.hasContent()) {
            for (ICompositeType attachment : libraryAdapter.getContent()) {
                IAttachmentAdapter attachmentAdapter = this.adapterFactory.createAttachment(attachment);
                if (attachmentAdapter.getContentType().equals(contentType)) {
                    // get externalized extension if present and add custom load data
                    return new ByteArrayInputStream(attachmentAdapter.getData());
                }
            }
        }

        return null;
    }

    protected abstract IBaseResource getLibrary(ModelIdentifier libraryIdentifier);
}
