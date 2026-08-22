package org.opencds.cqf.fhir.cql.cql2elm.content

import jakarta.xml.bind.JAXB
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.hl7.cql.model.ModelIdentifier
import org.hl7.cql.model.ModelInfoProvider
import org.hl7.elm_modelinfo.r1.ModelInfo
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory

/**
 * This class implements logic for extracting content from a FHIR Library resource and provides an
 * extension point for implementing the fetch of a FHIR library matching a specific identifier.
 */
abstract class BaseFhirModelInfoProvider
protected constructor(protected val adapterFactory: IAdapterFactory) : ModelInfoProvider {
    override fun load(modelIdentifier: ModelIdentifier): ModelInfo? {
        val `is` = getModelInfoContent(modelIdentifier, ModelInfoContentType.XML)
        return JAXB.unmarshal(`is`, ModelInfo::class.java)
    }

    protected fun getModelInfoContent(
        modelIdentifier: ModelIdentifier,
        modelInfoContentType: ModelInfoContentType,
    ): InputStream? {
        val library = this.getLibrary(modelIdentifier) ?: return null

        return this.getContentStream(library, modelInfoContentType.mimeType())
    }

    protected fun getContentStream(library: IBaseResource?, contentType: String?): InputStream? {
        val libraryAdapter = this.adapterFactory.createLibrary(library)

        if (libraryAdapter.hasContent()) {
            for (attachment in libraryAdapter.getContent<ICompositeType?>()) {
                val attachmentAdapter = this.adapterFactory.createAttachment(attachment)
                if (attachmentAdapter.getContentType() == contentType) {
                    // get externalized extension if present and add custom load data
                    return ByteArrayInputStream(attachmentAdapter.getData())
                }
            }
        }

        return null
    }

    protected abstract fun getLibrary(libraryIdentifier: ModelIdentifier): IBaseResource?
}
