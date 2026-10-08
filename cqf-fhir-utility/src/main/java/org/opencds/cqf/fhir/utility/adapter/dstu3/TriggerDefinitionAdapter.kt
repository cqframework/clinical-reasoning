package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.dstu3.model.TriggerDefinition
import org.hl7.fhir.instance.model.api.IBase
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter

class TriggerDefinitionAdapter(triggerDefinition: IBase) :
    BaseElementAdapter(FhirVersionEnum.DSTU3, triggerDefinition), ITriggerDefinitionAdapter {
    private val triggerDefinition: TriggerDefinition

    init {
        require(triggerDefinition is TriggerDefinition) {
            "object passed as triggerDefinition argument is not a TriggerDefinition data type"
        }
        this.triggerDefinition = triggerDefinition
    }

    override fun get(): TriggerDefinition {
        return triggerDefinition
    }

    override fun hasName(): Boolean {
        return get().hasEventName()
    }

    override val name: String?
        get() {
            return get().getEventName()
        }

    override fun hasType(): Boolean {
        return get().hasType()
    }

    override val type: String?
        get() {
            return get().type.toCode()
        }
}
