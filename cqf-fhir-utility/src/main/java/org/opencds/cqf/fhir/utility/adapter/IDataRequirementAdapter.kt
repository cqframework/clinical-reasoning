package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IPrimitiveType

interface IDataRequirementAdapter : IAdapter<IBase> {
    fun hasId(): Boolean

    val id: String?

    fun hasType(): Boolean

    val type: String?

    fun hasProfile(): Boolean

    val profile: MutableList<IPrimitiveType<String?>?>?

    fun hasCodeFilter(): Boolean

    val codeFilter: MutableList<IDataRequirementCodeFilterAdapter?>?
}
