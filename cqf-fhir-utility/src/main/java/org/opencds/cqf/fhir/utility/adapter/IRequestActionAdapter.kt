package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.*

interface IRequestActionAdapter : IAdapter<IBase> {

    val id: String?

    fun setId(id: String?): IRequestActionAdapter?

    fun hasTitle(): Boolean

    val title: String?

    fun setTitle(title: String?): IRequestActionAdapter?

    fun hasDescription(): Boolean

    val description: String?

    fun setDescription(description: String?): IRequestActionAdapter?

    fun hasTextEquivalent(): Boolean

    val textEquivalent: String?

    fun setTextEquivalent(text: String?): IRequestActionAdapter?

    fun hasPriority(): Boolean

    val priority: String?

    fun setPriority(priority: String?): IRequestActionAdapter?

    fun hasCode(): Boolean

    val code: ICodeableConceptAdapter?

    fun setCode(code: ICodeableConceptAdapter?): IRequestActionAdapter?

    fun hasDocumentation(): Boolean

    fun <T> getDocumentation(): MutableList<T?>? where T : ICompositeType, T : IBaseHasExtensions

    fun <T> setDocumentation(documentation: MutableList<T?>?): IRequestActionAdapter? where
    T : ICompositeType,
    T : IBaseHasExtensions

    fun hasCondition(): Boolean

    fun <T : IBaseBackboneElement> getCondition(): MutableList<T?>?

    fun addCondition(condition: IBaseBackboneElement?)

    fun addCondition(conditionResult: Pair<IAdapter<*>, Boolean?>)

    fun getConditionResult(result: Boolean?): IBaseExtension<*, *>

    fun hasRelatedAction(): Boolean

    fun <T : IBaseBackboneElement> getRelatedAction(): MutableList<T?>?

    fun addRelatedAction(relatedAction: IBaseBackboneElement?)

    fun hasTiming(): Boolean

    val timing: IBaseDatatype?

    fun setTiming(timing: IBaseDatatype?): IRequestActionAdapter?

    fun hasType(): Boolean

    val type: ICodeableConceptAdapter?

    fun setType(type: ICodeableConceptAdapter?): IRequestActionAdapter?

    fun hasSelectionBehavior(): Boolean

    val selectionBehavior: String?

    fun setSelectionBehavior(behavior: String?): IRequestActionAdapter?

    fun hasResource(): Boolean

    val resource: IBaseReference?

    fun setResource(resource: IBaseReference?): IRequestActionAdapter?

    fun hasAction(): Boolean

    val action: MutableList<IRequestActionAdapter?>?

    fun addAction(action: IBaseBackboneElement?)

    fun setAction(actions: MutableList<IRequestActionAdapter?>?): IRequestActionAdapter?
}
