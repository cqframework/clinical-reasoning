package org.opencds.cqf.fhir.utility.visitor.r5;

import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.adapter.r5.r5LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Parameters;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

import java.util.List;


public class KnowledgeArtifactReleaseVisitor implements r5KnowledgeArtifactVisitor {
  @Override
  public IBase visit(r5LibraryAdapter library, Repository repository, Parameters operationParameters) {
    return new OperationOutcome();
  }
  @Override
  public IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
    return new OperationOutcome();
  }
  @Override
  public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    return new OperationOutcome();
  }
  @Override
  public IBase visit(IBaseLibraryAdapter library, Repository repository, IBaseParameters operationParameters) {
    return new OperationOutcome();
  }
  @Override
  public IBase visit(IBaseKnowledgeArtifactAdapter library, Repository repository, IBaseParameters operationParameters) {
    return new OperationOutcome();
  }
}