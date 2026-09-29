package org.opencds.cqf.fhir.utility.adapter

import java.util.*
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.instance.model.api.IPrimitiveType

/** This interface exposes common functionality across all FHIR Questionnaire versions. */
interface IQuestionnaireResponseAdapter : IResourceAdapter {
    override fun setId(id: String): IQuestionnaireResponseAdapter?

    fun hasQuestionnaire(): Boolean

    val questionnaire: String?

    val questionnaireCanonical: IPrimitiveType<String?>?

    fun setQuestionnaire(canonical: String?): IQuestionnaireResponseAdapter?

    fun hasSubject(): Boolean

    val subject: IIdType?

    fun setSubject(subject: IIdType?): IQuestionnaireResponseAdapter?

    fun setAuthored(date: Date?): IQuestionnaireResponseAdapter?

    fun setStatus(status: String?): IQuestionnaireResponseAdapter?

    fun hasItem(): Boolean

    fun hasItem(linkId: String): Boolean {
        return getItem(linkId).isNotEmpty()
    }

    var item: MutableList<IQuestionnaireResponseItemComponentAdapter?>?

    fun getItem(linkId: String): MutableList<IQuestionnaireResponseItemComponentAdapter?> {
        return getItemsWithLinkId(this.item!!, linkId)
    }

    fun getItemsWithLinkId(
        items: List<IQuestionnaireResponseItemComponentAdapter?>,
        linkId: String,
    ): MutableList<IQuestionnaireResponseItemComponentAdapter?> {
        val matchingItems = items.filter { i -> linkId == i!!.linkId }.toMutableList()
        items.forEach { i ->
            if (i!!.hasItem()) {
                matchingItems.addAll(
                    getItemsWithLinkId(
                        i.item!!
                            .map { obj ->
                                IQuestionnaireResponseItemComponentAdapter::class.java.cast(obj)
                            }
                            .toList(),
                        linkId,
                    )
                )
            }
        }
        return matchingItems
    }

    fun addItem(item: IQuestionnaireResponseItemComponentAdapter?)

    fun addItems(items: MutableList<IQuestionnaireResponseItemComponentAdapter?>?)
}
