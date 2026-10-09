package org.opencds.cqf.fhir.utility.adapter.r5

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r5.model.TriggerDefinition
import org.opencds.cqf.fhir.utility.adapter.BaseElementAdapter
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter

class TriggerDefinitionAdapter(triggerDefinition: IBase) :
    BaseElementAdapter(FhirVersionEnum.R5, triggerDefinition), ITriggerDefinitionAdapter {
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
        return get().hasName()
    }

    override val name: String?
        get() {
            return get().name
        }

    override fun hasType(): Boolean {
        return get().hasType()
    }

    override val type: String?
        get() {
            return get().type.toCode()
        }
}
