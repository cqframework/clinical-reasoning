package org.opencds.cqf.fhir.utility.visitor;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;

public interface KnowledgeArtifactVisitor {
    IBase visit(KnowledgeArtifactAdapter knowledgeArtifact, Repository repository, IBaseParameters draftParameters);

    IBase visit(LibraryAdapter library, Repository repository, IBaseParameters draftParameters);

    IBase visit(PlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters draftParameters);

    IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters draftParameters);
}
