package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter

class RequestActionAdapter(requestAction: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, requestAction), IRequestActionAdapter {
    private val requestAction: RequestGroup.RequestGroupActionComponent

    init {
        require(requestAction is RequestGroup.RequestGroupActionComponent) {
            "element passed as action argument is not a RequestGroupActionComponent Element"
        }
        this.requestAction = requestAction
    }

    override fun get(): RequestGroup.RequestGroupActionComponent {
        return requestAction
    }

    override val id: String?
        get() {
            return get().id
        }

    override fun setId(id: String?): IRequestActionAdapter {
        get().setId(id)
        return this
    }

    override fun hasTitle(): Boolean {
        return get().hasTitle()
    }

    override val title: String?
        get() {
            return get().title
        }

    override fun setTitle(title: String?): IRequestActionAdapter {
        get().setTitle(title)
        return this
    }

    override fun hasDescription(): Boolean {
        return get().hasDescription()
    }

    override val description: String?
        get() {
            return get().description
        }

    override fun setDescription(description: String?): IRequestActionAdapter {
        get().setDescription(description)
        return this
    }

    override fun hasTextEquivalent(): Boolean {
        return get().hasTextEquivalent()
    }

    override val textEquivalent: String?
        get() {
            return get().textEquivalent
        }

    override fun setTextEquivalent(text: String?): IRequestActionAdapter {
        get().setTextEquivalent(text)
        return this
    }

    override fun hasPriority(): Boolean {
        return false
    }

    override val priority: String?
        get() {
            return null
        }

    override fun setPriority(priority: String?): IRequestActionAdapter {
        return this
    }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override val code: ICodeableConceptAdapter
        get() {
            return adapterFactory.createCodeableConcept(get().code.get(0))
        }

    override fun setCode(code: ICodeableConceptAdapter?): IRequestActionAdapter {
        get().setCode(if (code == null) null else mutableListOf(code.get() as CodeableConcept?))
        return this
    }

    override fun hasDocumentation(): Boolean {
        return get().hasDocumentation()
    }

    override fun <T> getDocumentation(): MutableList<T?>? where
    T : ICompositeType,
    T : IBaseHasExtensions {
        @Suppress("UNCHECKED_CAST")
        return get().documentation as MutableList<T?>?
    }

    override fun <T> setDocumentation(documentation: MutableList<T?>?): IRequestActionAdapter where
    T : ICompositeType,
    T : IBaseHasExtensions {
        get()
            .setDocumentation(
                documentation!!.map { obj -> RelatedArtifact::class.java.cast(obj) }.toMutableList()
            )
        return this
    }

    override fun hasCondition(): Boolean {
        return get().hasCondition()
    }

    override fun <T : IBaseBackboneElement> getCondition(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return get().condition as MutableList<T?>?
    }

    override fun addCondition(element: IBaseBackboneElement?) {
        if (element is PlanDefinition.PlanDefinitionActionConditionComponent) {
            get()
                .addCondition(
                    RequestGroup.RequestGroupActionConditionComponent()
                        .setKind(RequestGroup.ActionConditionKind.fromCode(element.kind.toCode()))
                        .setExpression(element.expression)
                )
        }
    }

    override fun hasRelatedAction(): Boolean {
        return get().hasRelatedAction()
    }

    override fun <T : IBaseBackboneElement> getRelatedAction(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return get().relatedAction as MutableList<T?>?
    }

    override fun addRelatedAction(element: IBaseBackboneElement?) {
        if (element is PlanDefinition.PlanDefinitionActionRelatedActionComponent) {
            get()
                .addRelatedAction(
                    RequestGroup.RequestGroupActionRelatedActionComponent()
                        .setActionId(element.getActionId())
                        .setRelationship(
                            RequestGroup.ActionRelationshipType.fromCode(
                                element.relationship.toCode()
                            )
                        )
                        .setOffset(element.offset)
                )
        }
    }

    override fun hasTiming(): Boolean {
        return get().hasTiming()
    }

    override val timing: IBaseDatatype?
        get() {
            return get().timing
        }

    override fun setTiming(timing: IBaseDatatype?): IRequestActionAdapter {
        get().setTiming(timing as Type?)
        return this
    }

    override fun hasType(): Boolean {
        return get().hasType()
    }

    override val type: ICodeableConceptAdapter
        get() {
            return CodeableConceptAdapter(CodeableConcept().addCoding(get().type))
        }

    override fun setType(type: ICodeableConceptAdapter?): IRequestActionAdapter {
        get().setType((type!!.get() as CodeableConcept).getCodingFirstRep())
        return this
    }

    override fun hasSelectionBehavior(): Boolean {
        return get().hasSelectionBehavior()
    }

    override val selectionBehavior: String?
        get() {
            return get().selectionBehavior.toCode()
        }

    override fun setSelectionBehavior(behavior: String?): IRequestActionAdapter {
        get().setSelectionBehavior(RequestGroup.ActionSelectionBehavior.fromCode(behavior))
        return this
    }

    override fun hasResource(): Boolean {
        return get().hasResource()
    }

    override val resource: Reference?
        get() {
            return get().resource
        }

    override fun setResource(resource: IBaseReference?): IRequestActionAdapter {
        get().setResource(resource as Reference?)
        return this
    }

    override fun hasAction(): Boolean {
        return get().hasAction()
    }

    override val action: MutableList<IRequestActionAdapter?>
        get() {
            return get()
                .action
                .map { requestAction -> RequestActionAdapter(requestAction) }
                .toMutableList()
        }

    override fun addAction(element: IBaseBackboneElement?) {
        if (element is RequestGroup.RequestGroupActionComponent) {
            get().addAction(element)
        }
    }

    override fun setAction(actions: MutableList<IRequestActionAdapter?>?): IRequestActionAdapter {
        get()
            .setAction(
                if (actions == null) null
                else
                    actions
                        .map { obj -> obj!!.get() }
                        .map { obj ->
                            RequestGroup.RequestGroupActionComponent::class.java.cast(obj)
                        }
                        .toMutableList()
            )
        return this
    }
}
