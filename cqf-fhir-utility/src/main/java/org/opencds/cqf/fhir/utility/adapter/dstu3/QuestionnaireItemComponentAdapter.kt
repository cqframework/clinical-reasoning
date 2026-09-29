package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.Coding
import org.hl7.fhir.dstu3.model.Questionnaire
import org.hl7.fhir.dstu3.model.QuestionnaireResponse
import org.hl7.fhir.dstu3.model.Type
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseCoding
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.adapter.*

class QuestionnaireItemComponentAdapter(item: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, item), IQuestionnaireItemComponentAdapter {
    private val _item: Questionnaire.QuestionnaireItemComponent

    init {
        require(item is Questionnaire.QuestionnaireItemComponent) {
            "object passed as item argument is not a QuestionnaireItemComponent data type"
        }
        _item = item
    }

    override fun get(): Questionnaire.QuestionnaireItemComponent {
        return _item
    }

    override val linkId: String?
        get() {
            return _item.linkId
        }

    override fun setLinkId(linkId: String?): IQuestionnaireItemComponentAdapter {
        get().setLinkId(linkId)
        return this
    }

    override fun hasDefinition(): Boolean {
        return _item.hasDefinition()
    }

    override val definition: String?
        get() {
            return _item.definition
        }

    override fun setDefinition(definition: String?): IQuestionnaireItemComponentAdapter {
        get().setDefinition(definition)
        return this
    }

    override fun hasItem(): Boolean {
        return _item.hasItem()
    }

    override var item: MutableList<out IItemComponentAdapter?>?
        get() {
            return _item.item
                .map { questionnaireItem ->
                    adapterFactory.createQuestionnaireItem(questionnaireItem)
                }
                .toMutableList()
        }
        set(items) {
            _item.setItem(
                items!!
                    .map { obj -> obj!!.get() }
                    .map { obj -> Questionnaire.QuestionnaireItemComponent::class.java.cast(obj) }
                    .toMutableList()
            )
        }

    override fun addItem(item: IItemComponentAdapter?) {
        this._item.addItem(item!!.get() as Questionnaire.QuestionnaireItemComponent?)
    }

    override fun addItems(items: MutableList<IQuestionnaireItemComponentAdapter?>?) {
        items!!
            .map { obj -> obj!!.get() }
            .map { obj -> Questionnaire.QuestionnaireItemComponent::class.java.cast(obj) }
            .forEach { t -> this._item.addItem(t) }
    }

    override val code: MutableList<IBaseCoding?>
        get() {
            return _item.code.map { obj -> IBaseCoding::class.java.cast(obj) }.toMutableList()
        }

    override val text: String?
        get() {
            return _item.text
        }

    override fun setText(text: String?): IQuestionnaireItemComponentAdapter {
        _item.setText(text)
        return this
    }

    override val type: String?
        get() {
            return _item.type.toCode()
        }

    override fun setType(type: String?): IQuestionnaireItemComponentAdapter {
        _item.setType(Questionnaire.QuestionnaireItemType.fromCode(type))
        return this
    }

    override val isGroupItem: Boolean
        get() {
            return _item.type == Questionnaire.QuestionnaireItemType.GROUP
        }

    override val isChoiceItem: Boolean
        get() {
            return _item.type == Questionnaire.QuestionnaireItemType.CHOICE
        }

    override val required: Boolean
        get() {
            return _item.required
        }

    override fun setRequired(required: Boolean): IQuestionnaireItemComponentAdapter {
        get().setRequired(required)
        return this
    }

    override val repeats: Boolean
        get() {
            return _item.repeats
        }

    override fun setRepeats(repeats: Boolean): IQuestionnaireItemComponentAdapter {
        get().setRepeats(repeats)
        return this
    }

    override fun addAnswerOption(option: ICodingAdapter?) {
        get().addOption().setValue(option!!.get() as Coding?)
    }

    override fun hasInitial(): Boolean {
        return _item.hasInitial()
    }

    override val initial: MutableList<Type?>
        get() {
            return if (_item.hasInitial()) mutableListOf<Type?>(_item.initial) else mutableListOf()
        }

    override fun newResponseItem(): IQuestionnaireResponseItemComponentAdapter {
        return adapterFactory.createQuestionnaireResponseItem(
            QuestionnaireResponse.QuestionnaireResponseItemComponent()
                .setLinkId(_item.linkId)
                .setDefinitionElement(_item.definitionElement)
                .setTextElement(_item.textElement)
        )
    }

    override fun newExpression(language: String?, expression: String?): ICompositeType? {
        return null
    }
}
