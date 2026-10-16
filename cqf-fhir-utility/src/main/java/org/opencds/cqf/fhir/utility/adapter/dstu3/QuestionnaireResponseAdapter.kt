package org.opencds.cqf.fhir.utility.adapter.dstu3

import java.util.*
import org.hl7.fhir.dstu3.model.QuestionnaireResponse
import org.hl7.fhir.dstu3.model.Reference
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.dstu3.model.StringType
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.instance.model.api.IIdType
import org.opencds.cqf.fhir.utility.Ids.newId
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter

class QuestionnaireResponseAdapter : ResourceAdapter, IQuestionnaireResponseAdapter {
    constructor(questionnaireResponse: IDomainResource) : super(questionnaireResponse as Resource) {
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
        get().id = id
        return this
    }

    override fun hasQuestionnaire(): Boolean {
        return get().hasQuestionnaire()
    }

    override val questionnaire: String?
        get() {
            return get().questionnaire.reference
        }

    override val questionnaireCanonical: StringType?
        get() {
            return if (get().hasQuestionnaire()) get().questionnaire.getReferenceElement_()
            else null
        }

    override fun setQuestionnaire(canonical: String?): IQuestionnaireResponseAdapter {
        get().questionnaire = Reference(canonical)
        return this
    }

    override fun hasSubject(): Boolean {
        return get().hasSubject()
    }

    override val subject: IIdType?
        get() {
            return if (get().hasSubject()) newId<IIdType?>(fhirVersion()!!, get().subject.reference)
            else null
        }

    override fun setSubject(subject: IIdType?): IQuestionnaireResponseAdapter {
        get().subject = Reference(subject)
        return this
    }

    override fun setAuthored(date: Date?): IQuestionnaireResponseAdapter {
        get().authored = date
        return this
    }

    override fun setStatus(status: String?): IQuestionnaireResponseAdapter {
        get().status = QuestionnaireResponse.QuestionnaireResponseStatus.fromCode(status)
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
            this.questionnaireResponse.item =
                items!!
                    .map { obj -> obj!!.get() }
                    .map { obj ->
                        QuestionnaireResponse.QuestionnaireResponseItemComponent::class
                            .java
                            .cast(obj)
                    }
                    .toMutableList()
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
