package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.Attachment
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter

internal class AttachmentAdapter(attachment: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, attachment), IAttachmentAdapter {
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
            this.attachment.contentType = contentType
        }

    override var data: ByteArray?
        get() {
            return this.attachment.data
        }
        set(data) {
            this.attachment.data = data
        }
}
