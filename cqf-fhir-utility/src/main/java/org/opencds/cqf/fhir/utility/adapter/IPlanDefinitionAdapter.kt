package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseBackboneElement

/** This interface exposes common functionality across all FHIR PlanDefinition versions. */
interface IPlanDefinitionAdapter : IKnowledgeArtifactAdapter {

    val description: String?

    fun hasLibrary(): Boolean

    val library: MutableList<String?>?

    fun hasGoal(): Boolean

    val goal: MutableList<IBaseBackboneElement?>?

    fun hasAction(): Boolean

    val action: MutableList<IPlanDefinitionActionAdapter?>?
}
