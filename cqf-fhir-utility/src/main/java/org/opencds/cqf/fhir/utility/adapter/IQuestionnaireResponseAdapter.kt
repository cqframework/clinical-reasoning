package org.opencds.cqf.fhir.utility.adapter

import java.util.Date
import org.hl7.fhir.instance.model.api.IBaseReference
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.instance.model.api.IPrimitiveType

/** This interface exposes common functionality across all FHIR Questionnaire versions. */
interface IQuestionnaireResponseAdapter : IResourceAdapter {
    override fun setId(id: String): IQuestionnaireResponseAdapter?

    fun hasQuestionnaire(): Boolean

    val questionnaire: String?

    val questionnaireCanonical: IPrimitiveType<String?>?

    fun setQuestionnaire(canonical: String?): IQuestionnaireResponseAdapter?

    val basedOn: MutableList<IBaseReference?>?
        get() {
            return resolvePathList(get(), "basedOn", IBaseReference::class.java)
        }

    val partOf: MutableList<IBaseReference?>?
        get() {
            return resolvePathList(get(), "partOf", IBaseReference::class.java)
        }

    fun hasSubject(): Boolean

    val subject: IIdType?

    fun setSubject(subject: IIdType?): IQuestionnaireResponseAdapter?

    fun hasEncounter(): Boolean {
        return encounter != null
    }

    val encounter: IBaseReference?
        get() {
            return resolvePath("encounter", IBaseReference::class.java)
        }

    fun setEncounter(encounter: IBaseReference?): IQuestionnaireResponseAdapter {
        setValue("encounter", encounter)
        return this
    }

    fun hasAuthored(): Boolean {
        return authored != null
    }

    val authored: Date?
        get() {
            val authored = resolvePath("authored", IPrimitiveType::class.java)
            return if (authored == null) null else authored.value as Date?
        }

    fun setAuthored(date: Date?): IQuestionnaireResponseAdapter?

    fun hasAuthor(): Boolean {
        return author != null
    }

    val author: IBaseReference?
        get() {
            return resolvePath("author", IBaseReference::class.java)
        }

    fun setStatus(status: String?): IQuestionnaireResponseAdapter?

    val status: String?
        get() {
            return resolvePathString("status")
        }

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
