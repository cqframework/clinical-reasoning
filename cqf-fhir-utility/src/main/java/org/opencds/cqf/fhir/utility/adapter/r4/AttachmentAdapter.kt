package org.opencds.cqf.fhir.utility.adapter.r4

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.ICompositeType
import org.hl7.fhir.r4.model.Attachment
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter

internal class AttachmentAdapter(attachment: IBase) :
    BaseElementAdapter(FhirVersionEnum.R4, attachment), IAttachmentAdapter {
    protected val attachment: Attachment

    init {
        require(attachment.fhirType() == "Attachment") {
            "resource passed as attachment argument is not an Attachment resource"
        }

        require(attachment is Attachment) {
            "attachment is incorrect fhir version for this adapter"
        }

        this.attachment = attachment
    }

    override fun get(): ICompositeType {
        return this.attachment
    }

    override var contentType: String?
        get() {
            return this.attachment.contentType
        }
        set(contentType) {
            this.attachment.setContentType(contentType)
        }

    override var data: ByteArray?
        get() {
            return this.attachment.data
        }
        set(data) {
            this.attachment.setData(data)
        }
}
