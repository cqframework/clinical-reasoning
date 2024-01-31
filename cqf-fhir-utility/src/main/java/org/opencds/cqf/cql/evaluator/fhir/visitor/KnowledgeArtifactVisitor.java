package org.opencds.cqf.cql.evaluator.fhir.visitor;

import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.api.Repository;

public interface KnowledgeArtifactVisitor {
  //	void visit(ActivityDefinitionAdapter activityDefinition);
  void visit(LibraryAdapter library, Repository theRepository);
  void visit(PlanDefinitionAdapter planDefinition, Repository theRepository);
//  //	void visit(StructureDefinitionAdapter structureDefinition);
  void visit(ValueSetAdapter valueSet, Repository theRepository); 
}

