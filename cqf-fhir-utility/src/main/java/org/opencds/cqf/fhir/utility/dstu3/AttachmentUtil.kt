package org.opencds.cqf.fhir.utility.dstu3

import org.hl7.fhir.dstu3.model.Library
import org.hl7.fhir.instance.model.api.IBaseResource

object AttachmentUtil {
    @JvmStatic
    fun getCqlLocation(resource: IBaseResource?): String? {
        val library = resource as Library
        for (attachment in library.content) {
            if (
                attachment.hasContentType() &&
                    attachment.contentType == "text/cql" &&
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
            for (attachment in library.content) {
                if (attachment.hasContentType() && attachment.contentType == "text/cql") {
                    attachment.setData(text.toByteArray())
                    library.addContent(attachment)
                    return library as IBaseResource
                }
            }
        }
        return null
    }
}
