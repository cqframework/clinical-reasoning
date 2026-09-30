package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase

interface ITriggerDefinitionAdapter : IAdapter<IBase> {
    fun hasName(): Boolean

    val name: String?

    fun hasType(): Boolean

    val type: String?
}
