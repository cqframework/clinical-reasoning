package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IIdentifierAdapter : IAdapter<IBase> {

    val value: String?

    fun hasValue(): Boolean

    val system: String?

    fun hasSystem(): Boolean
}
