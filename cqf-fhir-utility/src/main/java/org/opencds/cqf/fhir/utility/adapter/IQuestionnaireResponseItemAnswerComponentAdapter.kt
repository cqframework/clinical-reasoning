package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype

interface IQuestionnaireResponseItemAnswerComponentAdapter : IAdapter<IBase> {
    fun hasValue(): Boolean

    val value: IBase?

    fun setValue(value: IBaseDatatype?)

    fun hasItem(): Boolean

    var item: MutableList<IQuestionnaireResponseItemComponentAdapter?>?
}
