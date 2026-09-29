package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.QuestionnaireResponse
import org.hl7.fhir.dstu3.model.Type
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.opencds.cqf.fhir.utility.adapter.*

class QuestionnaireResponseItemComponentAdapter(item: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, item), IQuestionnaireResponseItemComponentAdapter {
    private val _item: QuestionnaireResponse.QuestionnaireResponseItemComponent

    init {
        require(item is QuestionnaireResponse.QuestionnaireResponseItemComponent) {
            "object passed as item argument is not a QuestionnaireResponseItemComponent data type"
        }
        _item = item
    }

    override fun get(): QuestionnaireResponse.QuestionnaireResponseItemComponent {
        return _item
    }

    override val linkId: String?
        get() {
            return _item.linkId
        }

    override fun setLinkId(linkId: String?): IQuestionnaireResponseItemComponentAdapter {
        get().setLinkId(linkId)
        return this
    }

    override fun setDefinition(definition: String?): IQuestionnaireResponseItemComponentAdapter {
        get().setDefinition(definition)
        return this
    }

    override fun hasDefinition(): Boolean {
        return _item.hasDefinition()
    }

    override val definition: String?
        get() {
            return _item.definition
        }

    override fun hasItem(): Boolean {
        return _item.hasItem()
    }

    override var item: MutableList<out IItemComponentAdapter?>?
        get() {
            return _item.item
                .map { questionnaireResponseItem ->
                    adapterFactory.createQuestionnaireResponseItem(questionnaireResponseItem)
                }
                .toMutableList()
        }
        set(items) {
            _item.setItem(
                items!!
                    .map { obj -> obj!!.get() }
                    .map { obj ->
                        QuestionnaireResponse.QuestionnaireResponseItemComponent::class
                            .java
                            .cast(obj)
                    }
                    .toMutableList()
            )
        }

    override fun addItem(item: IItemComponentAdapter?) {
        this._item.addItem(
            item!!.get() as QuestionnaireResponse.QuestionnaireResponseItemComponent?
        )
    }

    override fun addItems(items: MutableList<IQuestionnaireResponseItemComponentAdapter?>?) {
        items!!
            .map { obj -> obj!!.get() }
            .map { obj ->
                QuestionnaireResponse.QuestionnaireResponseItemComponent::class.java.cast(obj)
            }
            .forEach { t -> this._item.addItem(t) }
    }

    override fun hasAnswer(): Boolean {
        return _item.hasAnswer()
    }

    override var answer: MutableList<IQuestionnaireResponseItemAnswerComponentAdapter?>?
        get() {
            return _item.answer
                .map { questionnaireResponseItemAnswer ->
                    adapterFactory.createQuestionnaireResponseItemAnswer(
                        questionnaireResponseItemAnswer
                    )
                }
                .toMutableList()
        }
        set(answers) {
            _item.setAnswer(
                answers!!
                    .map { obj -> obj!!.get() }
                    .filter { o ->
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent::class
                            .java
                            .isInstance(o)
                    }
                    .map { obj ->
                        QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent::class
                            .java
                            .cast(obj)
                    }
                    .toMutableList()
            )
        }

    override fun newAnswer(
        value: IBaseDatatype?
    ): IQuestionnaireResponseItemAnswerComponentAdapter? {
        return adapterFactory.createQuestionnaireResponseItemAnswer(
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent()
                .setValue(value as Type?)
        )
    }
}
