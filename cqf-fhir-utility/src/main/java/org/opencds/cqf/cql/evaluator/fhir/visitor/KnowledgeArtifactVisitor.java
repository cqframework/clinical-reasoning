package org.opencds.cqf.cql.evaluator.fhir.visitor;

import org.opencds.cqf.cql.evaluator.fhir.adapter.LibraryAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ValueSetAdapter;

public interface KnowledgeArtifactVisitor {
  //	void visit(ActivityDefinitionAdapter activityDefinition);
  void visit(LibraryAdapter library);
  void visit(PlanDefinitionAdapter planDefinition);
//  //	void visit(StructureDefinitionAdapter structureDefinition);
  void visit(ValueSetAdapter valueSet);
}

