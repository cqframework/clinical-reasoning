package org.opencds.cqf.fhir.utility.r5

import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r5.model.Library

object AttachmentUtil {
    @JvmStatic
    fun getCqlLocation(resource: IBaseResource?): String? {
        val library = resource as Library
        for (attachment in library.getContent()) {
            if (
                attachment.hasContentType() &&
                    attachment.getContentType() == "text/cql" &&
                    !attachment.url.isNullOrBlank()
            ) {
                return attachment.url
            }
        }
        return null
    }

    @JvmStatic
    fun addData(resource: IBaseResource?, text: String?): IBaseResource? {
        if (!text.isNullOrBlank()) {
            val library = resource as Library
            for (attachment in library.getContent()) {
                if (attachment.hasContentType() && attachment.getContentType() == "text/cql") {
                    attachment.setData(text.toByteArray())
                    library.addContent(attachment)
                    return library as IBaseResource
                }
            }
        }
        return null
    }
}
