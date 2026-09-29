package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseBackboneElement

/** This interface exposes common functionality across all FHIR Questionnaire versions. */
interface IQuestionnaireAdapter : IKnowledgeArtifactAdapter {
    fun hasItem(): Boolean

    var item: MutableList<IQuestionnaireItemComponentAdapter?>?

    fun addItem(item: IBaseBackboneElement?)

    fun addItem(item: IQuestionnaireItemComponentAdapter?)

    fun addItems(items: MutableList<IQuestionnaireItemComponentAdapter?>?)

    val allItemDefinitions: MutableSet<String?>
        get() = getItemDefs(this.item!!)

    fun getItemDefs(items: MutableList<out IItemComponentAdapter?>): MutableSet<String?> {
        val defs = HashSet<String?>()
        items.forEach { item ->
            if (item!!.hasDefinition()) {
                defs.add(item.definition)
            }
            if (item.hasItem()) {
                defs.addAll(getItemDefs(item.item!!))
            }
        }
        return defs
    }
}
