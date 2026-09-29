package org.opencds.cqf.fhir.utility.adapter.r4

import java.util.*
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IIdType
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter

class QuestionnaireResponseAdapter : ResourceAdapter, IQuestionnaireResponseAdapter {
    constructor(questionnaireResponse: IDomainResource) : super(questionnaireResponse) {
        require(questionnaireResponse is QuestionnaireResponse) {
            "resource passed as questionnaire argument is not a QuestionnaireResponse resource"
        }
    }

    constructor(questionnaireResponse: QuestionnaireResponse) : super(questionnaireResponse)

    protected val questionnaireResponse: QuestionnaireResponse
        get() = resource as QuestionnaireResponse

    override fun get(): QuestionnaireResponse {
        return this.questionnaireResponse
    }

    override fun setId(id: String): IQuestionnaireResponseAdapter {
        get().setId(id)
        return this
    }

    override fun hasQuestionnaire(): Boolean {
        return get().hasQuestionnaire()
    }

    override val questionnaire: String?
        get() {
            return get().questionnaire
        }

    override val questionnaireCanonical: CanonicalType?
        get() {
            return if (get().hasQuestionnaire()) get().questionnaireElement else null
        }

    override fun setQuestionnaire(canonical: String?): IQuestionnaireResponseAdapter {
        get().setQuestionnaire(canonical)
        return this
    }

    override fun hasSubject(): Boolean {
        return get().hasSubject()
    }

    override val subject: IIdType?
        get() {
            return if (get().hasSubject()) get().subject.referenceElement else null
        }

    override fun setSubject(subject: IIdType?): IQuestionnaireResponseAdapter {
        get().setSubject(Reference(subject))
        return this
    }

    override fun setAuthored(date: Date?): IQuestionnaireResponseAdapter {
        get().setAuthored(date)
        return this
    }

    override fun setStatus(status: String?): IQuestionnaireResponseAdapter {
        get().setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(status))
        return this
    }

    override fun hasItem(): Boolean {
        return this.questionnaireResponse.hasItem()
    }

    override var item: MutableList<IQuestionnaireResponseItemComponentAdapter?>?
        get() {
            return this.questionnaireResponse.item
                .map { questionnaireResponseItem ->
                    adapterFactory.createQuestionnaireResponseItem(questionnaireResponseItem)
                }
                .toMutableList()
        }
        set(items) {
            this.questionnaireResponse.setItem(
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

    override fun addItem(item: IQuestionnaireResponseItemComponentAdapter?) {
        this.questionnaireResponse.addItem(
            item!!.get() as QuestionnaireResponse.QuestionnaireResponseItemComponent?
        )
    }

    override fun addItems(items: MutableList<IQuestionnaireResponseItemComponentAdapter?>?) {
        items!!
            .map { obj -> obj!!.get() }
            .map { obj ->
                QuestionnaireResponse.QuestionnaireResponseItemComponent::class.java.cast(obj)
            }
            .forEach { item -> this.questionnaireResponse.addItem(item) }
    }
}
