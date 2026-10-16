package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseCoding

interface ICodeableConceptAdapter : IAdapter<IBase> {
    fun hasCoding(): Boolean

    val coding: MutableList<ICodingAdapter?>

    fun hasCoding(code: String?): Boolean

    val codingFirstRep: ICodingAdapter?
        get() {
            val codings = this.coding
            return if (codings.isEmpty()) null else codings[0]
        }

    fun addCoding(system: String?, code: String?, display: String?): ICodeableConceptAdapter

    fun setCoding(coding: List<IBaseCoding?>): ICodeableConceptAdapter {
        setValue("coding", coding)
        return this
    }

    fun addCoding(coding: IBaseCoding?): ICodeableConceptAdapter {
        setValue("coding", mutableListOf(coding))
        return this
    }
}
