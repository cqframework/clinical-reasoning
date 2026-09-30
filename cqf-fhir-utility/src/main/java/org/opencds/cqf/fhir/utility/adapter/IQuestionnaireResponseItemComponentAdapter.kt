package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseDatatype

interface IQuestionnaireResponseItemComponentAdapter : IItemComponentAdapter {
    fun setLinkId(linkId: String?): IQuestionnaireResponseItemComponentAdapter?

    fun setDefinition(definition: String?): IQuestionnaireResponseItemComponentAdapter?

    fun addItems(items: MutableList<IQuestionnaireResponseItemComponentAdapter?>?)

    fun hasAnswer(): Boolean

    var answer: MutableList<IQuestionnaireResponseItemAnswerComponentAdapter?>?

    fun newAnswer(value: IBaseDatatype?): IQuestionnaireResponseItemAnswerComponentAdapter?
}
