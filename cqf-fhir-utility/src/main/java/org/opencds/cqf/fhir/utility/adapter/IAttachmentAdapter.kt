package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.ICompositeType

/** This interface exposes common functionality across all FHIR Attachment versions. */
interface IAttachmentAdapter : IAdapter<IBase> {
    override fun get(): ICompositeType

    var contentType: String?

    var data: ByteArray?
}
