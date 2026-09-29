package org.opencds.cqf.fhir.utility.adapter

import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseParameters

interface IKnowledgeArtifactVisitor {
    fun visit(
        knowledgeArtifact: IKnowledgeArtifactAdapter?,
        draftParameters: IBaseParameters?,
    ): IBase?
}
