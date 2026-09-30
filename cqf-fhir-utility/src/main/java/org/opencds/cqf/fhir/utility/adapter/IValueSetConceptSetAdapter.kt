package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IValueSetConceptSetAdapter : IAdapter<IBase> {
    fun hasConcept(): Boolean

    val concept: MutableList<IValueSetConceptReferenceAdapter?>?

    fun hasSystem(): Boolean

    val system: String?
}
