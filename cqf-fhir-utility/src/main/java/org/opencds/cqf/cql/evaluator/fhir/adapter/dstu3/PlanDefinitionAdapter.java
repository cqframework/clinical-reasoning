package org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.cql.evaluator.fhir.visitor.KnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.api.Repository;

class PlanDefinitionAdapter extends KnowledgeArtifactAdapter implements org.opencds.cqf.cql.evaluator.fhir.adapter.PlanDefinitionAdapter {

  private PlanDefinition planDefinition;

  public PlanDefinitionAdapter(PlanDefinition planDefinition) {
    super(planDefinition);

    if (!planDefinition.fhirType().equals("PlanDefinition")) {
      throw new IllegalArgumentException(
          "resource passed as planDefinition argument is not a PlanDefinition resource");
    }

    this.planDefinition = planDefinition;
  }

  public void accept(KnowledgeArtifactVisitor visitor, Repository theRepository) {
    visitor.visit(this, theRepository);
  }

  protected PlanDefinition getPlanDefinition() {
    return this.planDefinition;
  }

  @Override
  public IBaseResource get() {
    return this.planDefinition;
  }

  @Override
  public IIdType getId() {
    return this.getPlanDefinition().getIdElement();
  }

  @Override
  public void setId(IIdType id) {
    this.getPlanDefinition().setId(id);
  }

  @Override
  public String getName() {
    return this.getPlanDefinition().getName();
  }

  @Override
  public void setName(String name) {
    this.getPlanDefinition().setName(name);
  }

  @Override
  public String getUrl() {
    return this.getPlanDefinition().getUrl();
  }

  @Override
  public void setUrl(String url) {
    this.getPlanDefinition().setUrl(url);
  }

  @Override
  public String getVersion() {
    return this.getPlanDefinition().getVersion();
  }

  @Override
  public void setVersion(String version) {
    this.getPlanDefinition().setVersion(version);
  }

  @Override
  public List<DependencyInfo> getDependencies() {
    List<DependencyInfo> references = new ArrayList<>();

    /*
      relatedArtifact[].resource
      library[]
      action[]..trigger[].dataRequirement[].profile[]
      action[]..trigger[].dataRequirement[].codeFilter[].valueSet
      action[]..condition[].expression.reference
      action[]..input[].profile[]
      action[]..input[].codeFilter[].valueSet
      action[]..output[].profile[]
      action[]..output[].codeFilter[].valueSet
      action[]..definitionCanonical
      action[]..dynamicValue[].expression.reference
      extension[cpg-partOf]
     */

    // relatedArtifact[].resource
    references.addAll(getRelatedArtifactReferences(this.planDefinition, this.planDefinition.getRelatedArtifact()));

    // library[]
    List<Reference> libraries = this.planDefinition.getLibrary();
    for (Reference ref : libraries) {
      // TODO: Account for reference.identifier?
      DependencyInfo dependency = new DependencyInfo(this.planDefinition.getUrl(), ref.getReference());
      references.add(dependency);
    }

    // TODO: Complete retrieval from other elements. Ideally use $data-requirements code

    return references;
  }
}
