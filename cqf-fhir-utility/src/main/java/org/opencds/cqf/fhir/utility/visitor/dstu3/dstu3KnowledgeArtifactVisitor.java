package org.opencds.cqf.fhir.utility.visitor.dstu3;

import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.dstu3.Dstu3LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public interface dstu3KnowledgeArtifactVisitor extends KnowledgeArtifactVisitor {
    IBase visit(dstu3KnowledgeArtifactVisitor knowledgeArtifact, Repository repository, Parameters draftParameters);

    IBase visit(Dstu3LibraryAdapter library, Repository repository, Parameters draftParameters);

    IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters draftParameters);

    IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters draftParameters);
}
