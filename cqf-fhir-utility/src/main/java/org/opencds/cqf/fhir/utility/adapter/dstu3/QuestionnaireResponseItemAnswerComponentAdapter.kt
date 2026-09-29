package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.QuestionnaireResponse
import org.hl7.fhir.dstu3.model.Type
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter

class QuestionnaireResponseItemAnswerComponentAdapter(answer: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, answer),
    IQuestionnaireResponseItemAnswerComponentAdapter {
    private val answer: QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent

    init {
        require(answer is QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent) {
            "object passed as answer argument is not a QuestionnaireResponseItemAnswerComponent data type"
        }
        this.answer = answer
    }

    override fun get(): QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent {
        return answer
    }

    override fun hasValue(): Boolean {
        return answer.hasValue()
    }

    override val value: IBase?
        get() {
            return answer.value
        }

    override fun setValue(value: IBaseDatatype?) {
        answer.value = value as Type?
    }

    override fun hasItem(): Boolean {
        return answer.hasItem()
    }

    override var item: MutableList<IQuestionnaireResponseItemComponentAdapter?>?
        get() {
            return answer.item
                .map { questionnaireResponseItem ->
                    adapterFactory.createQuestionnaireResponseItem(questionnaireResponseItem)
                }
                .toMutableList()
        }
        set(items) {
            answer.item =
                items!!
                    .map { obj -> obj!!.get() }
                    .map { obj ->
                        QuestionnaireResponse.QuestionnaireResponseItemComponent::class
                            .java
                            .cast(obj)
                    }
                    .toMutableList()
        }
}
