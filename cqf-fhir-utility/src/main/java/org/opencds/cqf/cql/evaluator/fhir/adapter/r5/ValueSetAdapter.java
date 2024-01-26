package org.opencds.cqf.cql.evaluator.fhir.adapter.r5;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.model.ValueSet.ConceptSetComponent;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.cql.evaluator.fhir.visitor.KnowledgeArtifactVisitor;

import java.util.ArrayList;
import java.util.List;

class ValueSetAdapter extends KnowledgeArtifactAdapter implements org.opencds.cqf.cql.evaluator.fhir.adapter.ValueSetAdapter {

  private ValueSet valueSet;

  public ValueSetAdapter(IBaseResource valueSet) {
    super(valueSet);

    if (!valueSet.fhirType().equals("ValueSet")) {
      throw new IllegalArgumentException(
          "resource passed as valueSet argument is not a ValueSet resource");
    }

    this.valueSet = (ValueSet) valueSet;
  }

  public void accept(KnowledgeArtifactVisitor visitor) {
    visitor.visit(this);
  }

  protected ValueSet getValueSet() {
    return this.valueSet;
  }

  @Override
  public IBaseResource get() {
    return this.valueSet;
  }

  @Override
  public IIdType getId() {
    return this.getValueSet().getIdElement();
  }

  @Override
  public void setId(IIdType id) {
    this.getValueSet().setId(id);
  }

  @Override
  public String getName() {
    return this.getValueSet().getName();
  }

  @Override
  public void setName(String name) {
    this.getValueSet().setName(name);
  }

  @Override
  public String getUrl() {
    return this.getValueSet().getUrl();
  }

  @Override
  public void setUrl(String url) {
    this.getValueSet().setUrl(url);
  }

  @Override
  public String getVersion() {
    return this.getValueSet().getVersion();
  }

  @Override
  public void setVersion(String version) {
    this.getValueSet().setVersion(version);
  }

  @Override
  public List<DependencyInfo> getDependencies() {
    List<DependencyInfo> references = new ArrayList<>();

    /*
      compose.include[].valueSet
      compose.exclude[].valueSet
    */

    List<ConceptSetComponent> composeEntries = new ArrayList<>();
    composeEntries.addAll(this.valueSet.getCompose().getInclude());
    composeEntries.addAll(this.valueSet.getCompose().getExclude());

    for (ConceptSetComponent component : composeEntries) {
      if (component.hasValueSet()) {
        for (CanonicalType ct : component.getValueSet()) {
          DependencyInfo dependency = new DependencyInfo(this.valueSet.getUrl(), ct.getValue());
          references.add(dependency);
        }
      }
    }

    // TODO: Ideally this would use the $data-requirements code
    return references;
  }
}
