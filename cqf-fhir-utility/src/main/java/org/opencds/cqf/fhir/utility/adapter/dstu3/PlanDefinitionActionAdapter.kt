package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.adapter.*

class PlanDefinitionActionAdapter(action: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, action), IPlanDefinitionActionAdapter {
    private val _action: PlanDefinition.PlanDefinitionActionComponent

    init {
        require(action is PlanDefinition.PlanDefinitionActionComponent) {
            "object passed as action argument is not a PlanDefinitionActionComponent data type"
        }
        _action = action
    }

    override fun get(): PlanDefinition.PlanDefinitionActionComponent {
        return _action
    }

    override fun hasId(): Boolean {
        return get().hasId()
    }

    override val id: String?
        get() {
            return get().id
        }

    override fun hasTitle(): Boolean {
        return get().hasTitle()
    }

    override val title: String?
        get() {
            return get().title
        }

    override fun hasDescription(): Boolean {
        return get().hasDescription()
    }

    override val description: String?
        get() {
            return get().description
        }

    override fun hasTextEquivalent(): Boolean {
        return get().hasTextEquivalent()
    }

    override val textEquivalent: String?
        get() {
            return get().textEquivalent
        }

    override fun hasPriority(): Boolean {
        return false
    }

    override val priority: String?
        get() {
            return null
        }

    override fun hasCode(): Boolean {
        return get().hasCode()
    }

    override val code: ICodeableConceptAdapter?
        get() {
            if (hasCode()) {
                return adapterFactory.createCodeableConcept(get().code.get(0))
            } else {
                return null
            }
        }

    override fun hasDocumentation(): Boolean {
        return get().hasDocumentation()
    }

    override fun <T> getDocumentation(): MutableList<T?> where
    T : ICompositeType,
    T : IBaseHasExtensions {
        @Suppress("UNCHECKED_CAST")
        return get().documentation.map { d -> d as T? }.toMutableList()
    }

    override fun hasTrigger(): Boolean {
        return get().hasTriggerDefinition()
    }

    override val trigger: MutableList<ITriggerDefinitionAdapter?>
        get() {
            return get()
                .triggerDefinition
                .map { triggerDefinition -> TriggerDefinitionAdapter(triggerDefinition) }
                .toMutableList()
        }

    override val triggerType: MutableList<String?>
        get() {
            return get().triggerDefinition.map { t -> t!!.type.toCode() }.toMutableList()
        }

    override fun hasCondition(): Boolean {
        return get().hasCondition()
    }

    override fun <T : IBaseBackboneElement> getCondition(): MutableList<T?>? {
        @Suppress("UNCHECKED_CAST")
        return get().condition as MutableList<T?>?
    }

    override fun hasInput(): Boolean {
        return get().hasInput()
    }

    override val inputDataRequirement: MutableList<IDataRequirementAdapter?>
        get() {
            return get()
                .input
                .map { dataRequirement -> adapterFactory.createDataRequirement(dataRequirement) }
                .toMutableList()
        }

    override fun hasRelatedAction(): Boolean {
        return get().hasRelatedAction()
    }

    override fun <T : IBaseBackboneElement> getRelatedAction(): MutableList<T?> {
        @Suppress("UNCHECKED_CAST")
        return get().relatedAction as MutableList<T?>
    }

    override fun hasTiming(): Boolean {
        return get().hasTiming()
    }

    override val timing: IBaseDatatype?
        get() {
            return get().timing
        }

    override fun hasType(): Boolean {
        return get().hasType()
    }

    override val type: ICodeableConceptAdapter
        get() {
            return adapterFactory.createCodeableConcept(CodeableConcept().addCoding(get().type))
        }

    override fun hasSelectionBehavior(): Boolean {
        return get().hasSelectionBehavior()
    }

    override val selectionBehavior: String?
        get() {
            if (hasSelectionBehavior()) {
                return get().selectionBehavior.toCode()
            } else {
                return null
            }
        }

    override fun hasDefinition(): Boolean {
        return get().hasDefinition()
    }

    override val definition: IPrimitiveType<String?>?
        get() {
            if (hasDefinition() && get().definition.hasReference()) {
                return get().definition.getReferenceElement_()
            } else {
                return null
            }
        }

    override fun hasAction(): Boolean {
        return get().hasAction()
    }

    override val action: MutableList<IPlanDefinitionActionAdapter?>
        get() {
            return get()
                .action
                .map { action -> adapterFactory.createPlanDefinitionAction(action) }
                .toMutableList()
        }

    override fun newRequestAction(): IRequestActionAdapter {
        return adapterFactory.createRequestAction(RequestGroup.RequestGroupActionComponent())
    }
}
