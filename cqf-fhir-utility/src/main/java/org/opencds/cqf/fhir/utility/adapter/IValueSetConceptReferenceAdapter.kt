package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IValueSetConceptReferenceAdapter : IAdapter<IBase> {
    fun hasCode(): Boolean

    val code: String?

    val display: String?
}
