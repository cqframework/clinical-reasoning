package org.opencds.cqf.fhir.utility.visitor.dstu3;

import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBaseLibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IBasePlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ValueSetAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;

import java.util.List;


public class KnowledgeArtifactReleaseVisitor implements KnowledgeArtifactVisitor {
  @Override
  public IBase visit(IBaseKnowledgeArtifactAdapter library, Repository repository, IBaseParameters operationParameters) {
    return new OperationOutcome();
  }
  @Override
  public IBase visit(IBaseLibraryAdapter library, Repository repository, IBaseParameters operationParameters) {
    // DependencyInfo --document here that there is a need for figuring out how to determine which package the dependency is in.
      // what is dependency, where did it originate? potentially the package?

    List<DependencyInfo> dependencies = library.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();

    // TODO: Use a HAPI FHIR Repository to search for the referenced resource, update it and visit it
    // load resource for those
    // update resources (per $release)
    // $expand valuesets (so that we can recursively pin versions for any resources referenced (e.g., CodySystem references from ValueSets that we don't own)
    // The expansion will not be persisted, it is only done as a step to pin all reference versions and record them in the manifest and root artifact (as 'depends-on' relatedArtifacts)
      // Release needs to take in a Parameters for Manifest?

  }
  @Override
  public IBase visit(IBasePlanDefinitionAdapter planDefinition, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = planDefinition.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }
  @Override
  public IBase visit(ValueSetAdapter valueSet, Repository repository, IBaseParameters operationParameters) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
    return new OperationOutcome();
  }
}