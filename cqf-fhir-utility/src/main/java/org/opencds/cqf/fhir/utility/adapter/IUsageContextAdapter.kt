package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface IUsageContextAdapter : IAdapter<IBase> {
    fun hasCode(): Boolean

    val code: ICodingAdapter?

    fun setCode(code: ICodingAdapter?): IUsageContextAdapter?

    fun hasValue(): Boolean

    fun hasValueCodeableConcept(): Boolean

    val valueCodeableConcept: ICodeableConceptAdapter?

    fun equalsDeep(other: IUsageContextAdapter?): Boolean
}
