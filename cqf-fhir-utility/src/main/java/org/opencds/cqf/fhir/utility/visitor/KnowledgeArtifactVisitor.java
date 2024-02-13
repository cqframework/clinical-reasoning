package org.opencds.cqf.fhir.utility.visitor;

import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.api.Repository;

public interface KnowledgeArtifactVisitor {
  //	void visit(ActivityDefinitionAdapter activityDefinition);
  IBase visit(IBaseKnowledgeArtifactAdapter knowledgeArtifact, Repository theRepository, IBaseParameters draftParameters);
  IBase visit(IBaseLibraryAdapter library, Repository theRepository, IBaseParameters draftParameters);
  IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository theRepository, IBaseParameters draftParameters);
//  //	void visit(StructureDefinitionAdapter structureDefinition);
IBase visit(ValueSetAdapter valueSet, Repository theRepository, IBaseParameters draftParameters); 
}

