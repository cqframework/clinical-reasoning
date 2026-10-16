package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IPrimitiveType

interface IDataRequirementCodeFilterAdapter : IAdapter<IBase> {
    fun hasCode(): Boolean

    val code: MutableList<ICodingAdapter?>?

    fun hasPath(): Boolean

    val path: String?

    fun hasValueSet(): Boolean

    val valueSet: IPrimitiveType<String?>?
}
