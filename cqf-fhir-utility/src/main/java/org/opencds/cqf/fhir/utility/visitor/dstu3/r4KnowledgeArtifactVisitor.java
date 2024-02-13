package org.opencds.cqf.fhir.utility.visitor.dstu3;

import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.r4LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;

public interface dstu3KnowledgeArtifactVisitor extends KnowledgeArtifactVisitor{
  //	void visit(ActivityDefinitionAdapter activityDefinition);
  IBase visit(dstu3KnowledgeArtifactVisitor knowledgeArtifact, Repository theRepository, IBaseParameters draftParameters);
  IBase visit(LibraryAdapter library, Repository theRepository, IBaseParameters draftParameters);
  IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository theRepository, IBaseParameters draftParameters);
//  //	void visit(StructureDefinitionAdapter structureDefinition);
IBase visit(ValueSetAdapter valueSet, Repository theRepository, IBaseParameters draftParameters); 
}

