package org.opencds.cqf.cql.evaluator.fhir.visitor.r5;

import org.opencds.cqf.cql.evaluator.fhir.adapter.LibraryAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.PlanDefinitionAdapter;
import org.opencds.cqf.cql.evaluator.fhir.adapter.ValueSetAdapter;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.cql.evaluator.fhir.visitor.KnowledgeArtifactVisitor;

import java.util.List;


public class KnowledgeArtifactReleaseVisitor implements KnowledgeArtifactVisitor {

  public void visit(LibraryAdapter library) {
    // DependencyInfo --document here that there is a need for figuring out how to determine which package the dependency is in.
      // what is dependency, where did it originate? potentially the package?

    List<DependencyInfo> dependencies = library.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }

    // TODO: Use a HAPI FHIR Repository to search for the referenced resource, update it and visit it
    // load resource for those
    // update resources (per $release)
    // $expand valuesets (so that we can recursively pin versions for any resources referenced (e.g., CodySystem references from ValueSets that we don't own)
    // The expansion will not be persisted, it is only done as a step to pin all reference versions and record them in the manifest and root artifact (as 'depends-on' relatedArtifacts)
      // Release needs to take in a Parameters for Manifest?

  }

  public void visit(PlanDefinitionAdapter planDefinition) {
    List<DependencyInfo> dependencies = planDefinition.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
  }

  public void visit(ValueSetAdapter valueSet) {
    List<DependencyInfo> dependencies = valueSet.getDependencies();
    for (DependencyInfo dependency : dependencies) {
      System.out.println(String.format("'%s' references '%s'", dependency.getReferenceSource(), dependency.getReference()));
    }
  }
}