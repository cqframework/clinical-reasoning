package org.opencds.cqf.fhir.utility.adapter

/** This interface exposes common functionality across all FHIR ActivityDefinition versions. */
interface IActivityDefinitionAdapter : IKnowledgeArtifactAdapter {

    val description: String?

    fun hasLibrary(): Boolean

    val library: MutableList<String?>?
}
