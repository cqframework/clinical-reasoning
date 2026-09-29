package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IValueSetExpansionContainsAdapter : IAdapter<IBase> {
    fun hasCode(): Boolean

    val code: String?

    fun hasSystem(): Boolean

    val system: String?

    fun hasDisplay(): Boolean

    val display: String?
}
