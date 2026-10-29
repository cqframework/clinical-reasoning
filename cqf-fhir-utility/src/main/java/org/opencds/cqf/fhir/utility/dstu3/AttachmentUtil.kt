package org.opencds.cqf.fhir.utility.dstu3

import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.dstu3.model.Library
import org.hl7.fhir.instance.model.api.IBaseResource

object AttachmentUtil {
    @JvmStatic
    fun getCqlLocation(resource: IBaseResource?): String? {
        val library = resource as Library
        for (attachment in library.getContent()) {
            if (
                attachment.hasContentType() &&
                    attachment.getContentType() == "text/cql" &&
                    StringUtils.isNotBlank(attachment.getUrl())
            ) {
                return attachment.getUrl()
            }
        }
        return null
    }

    @JvmStatic
    fun addData(resource: IBaseResource?, text: String?): IBaseResource? {
        if (StringUtils.isNotBlank(text)) {
            val library = resource as Library
            for (attachment in library.getContent()) {
                if (attachment.hasContentType() && attachment.getContentType() == "text/cql") {
                    attachment.setData(text!!.toByteArray())
                    library.addContent(attachment)
                    return library as IBaseResource
                }
            }
        }
        return null
    }
}
