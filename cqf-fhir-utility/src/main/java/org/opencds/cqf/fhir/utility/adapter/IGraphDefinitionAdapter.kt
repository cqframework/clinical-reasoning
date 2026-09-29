package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBaseBackboneElement

/** This interface exposes common functionality across all FHIR GraphDefinition versions. */
interface IGraphDefinitionAdapter : IKnowledgeArtifactAdapter {
    // R4
    val backBoneElements: MutableList<IBaseBackboneElement?>?

    // R5
    val node: MutableList<IBaseBackboneElement?>?
}
