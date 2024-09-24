package org.opencds.cqf.fhir.utility.visitor;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;

public interface IKnowledgeArtifactVisitor {
    IBase visit(KnowledgeArtifactAdapter knowledgeArtifact, Repository repository, IBaseParameters draftParameters);
}
