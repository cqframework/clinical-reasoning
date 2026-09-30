package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface ICodeableConceptAdapter : IAdapter<IBase> {
    fun hasCoding(): Boolean

    val coding: MutableList<ICodingAdapter?>

    fun hasCoding(code: String?): Boolean

    val codingFirstRep: ICodingAdapter?
        get() {
            val codings = this.coding
            return if (codings.isEmpty()) null else codings[0]
        }
}
