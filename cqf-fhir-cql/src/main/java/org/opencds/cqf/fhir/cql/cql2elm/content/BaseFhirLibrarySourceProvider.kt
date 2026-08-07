package org.opencds.cqf.fhir.cql.cql2elm.content

import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.cqframework.cql.cql2elm.LibraryContentType
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory

/**
 * This class implements logic for extracting content from a FHIR Library resource and provides an
 * extension point for implementing the fetch of a FHIR library matching a specific identifier.
 */
abstract class BaseFhirLibrarySourceProvider
protected constructor(protected val adapterFactory: IAdapterFactory) : LibrarySourceProvider {
    override fun getLibraryContent(
        libraryIdentifier: VersionedIdentifier,
        libraryContentType: LibraryContentType,
    ): Source? {
        val library = this.getLibrary(libraryIdentifier) ?: return null

        val inputStream = this.getContentStream(library, libraryContentType.mimeType())
        return inputStream?.asSource()?.buffered()
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

    override fun getLibrarySource(libraryIdentifier: VersionedIdentifier): Source? {
        return getLibraryContent(libraryIdentifier, LibraryContentType.CQL)
    }

    protected abstract fun getLibrary(libraryIdentifier: VersionedIdentifier): IBaseResource?
}
