package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface IKnowledgeArtifactVisitor {
    IBase visit(IKnowledgeArtifactAdapter knowledgeArtifact, IBaseParameters draftParameters);
}
