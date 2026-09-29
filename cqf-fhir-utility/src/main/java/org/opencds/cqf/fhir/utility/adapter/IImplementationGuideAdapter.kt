package org.opencds.cqf.fhir.utility.adapter

import java.util.*
import org.hl7.fhir.instance.model.api.IBaseExtension
import org.hl7.fhir.instance.model.api.IPrimitiveType

/** This interface exposes common functionality across all FHIR Implementation Guide versions. */
interface IImplementationGuideAdapter : IKnowledgeArtifactAdapter {
    override var approvalDate: Date?
        get() {
            val approvalDateExt =
                getExtensionByUrl<IBaseExtension<*, *>>(
                    "http://hl7.org/fhir/StructureDefinition/artifact-approvalDate"
                )
            if (approvalDateExt == null) {
                return null
            } else if (approvalDateExt.value == null || approvalDateExt.value.isEmpty) {
                return null
            } else if (
                approvalDateExt.value is IPrimitiveType<*> &&
                    (approvalDateExt.value as IPrimitiveType<*>).value is Date
            ) {
                return (approvalDateExt.value as IPrimitiveType<*>).value as Date?
            }
            return null
        }
        set(approvalDate) {
            super.approvalDate = approvalDate
        }
}
