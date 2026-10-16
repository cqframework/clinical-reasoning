package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseCoding
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.instance.model.api.ICompositeType
import org.opencds.cqf.fhir.utility.Constants.CPG_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT
import org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT
import org.opencds.cqf.fhir.utility.Constants.SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT

interface IQuestionnaireItemComponentAdapter : IItemComponentAdapter {
    fun setLinkId(linkId: String?): IQuestionnaireItemComponentAdapter?

    fun setDefinition(definition: String?): IQuestionnaireItemComponentAdapter?

    fun addItems(items: MutableList<IQuestionnaireItemComponentAdapter?>?)

    val code: MutableList<IBaseCoding?>?

    val text: String?

    fun setText(text: String?): IQuestionnaireItemComponentAdapter?

    val type: String?

    fun setType(type: String?): IQuestionnaireItemComponentAdapter?

    val isGroupItem: Boolean

    val isChoiceItem: Boolean

    val required: Boolean

    fun setRequired(required: Boolean): IQuestionnaireItemComponentAdapter?

    val repeats: Boolean

    fun setRepeats(repeats: Boolean): IQuestionnaireItemComponentAdapter?

    fun addAnswerOption(option: ICodingAdapter?)

    fun hasInitial(): Boolean

    val initial: MutableList<out IBaseDatatype?>?

    fun newResponseItem(): IQuestionnaireResponseItemComponentAdapter?

    fun newExpression(language: String?, expression: String?): ICompositeType?

    fun newExpression(expression: String?): ICompositeType? {
        return newExpression("text/cql-expression", expression)
    }

    val isContextItem: Boolean
        get() {
            return hasExtension(SDC_QUESTIONNAIRE_ITEM_POPULATION_CONTEXT) ||
                hasExtension(SDC_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT) ||
                hasExtension(CPG_QUESTIONNAIRE_DEFINITION_POPULATION_CONTEXT)
        }
}
