package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface ICodingAdapter : IAdapter<IBase> {

    val code: String?

    fun hasCode(): Boolean

    fun setCode(code: String?): ICodingAdapter?

    val display: String?

    fun hasDisplay(): Boolean

    fun setDisplay(display: String?): ICodingAdapter?

    val system: String?

    fun hasSystem(): Boolean

    fun setSystem(system: String?): ICodingAdapter?
}
