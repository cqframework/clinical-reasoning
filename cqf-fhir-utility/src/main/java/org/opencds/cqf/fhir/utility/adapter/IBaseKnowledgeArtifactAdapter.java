package org.opencds.cqf.fhir.utility.adapter;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public interface IBaseKnowledgeArtifactAdapter extends ResourceAdapter {

  IBaseResource get();

  default IIdType getId() {
    return this.get().getIdElement();
  }
  default void setId(IIdType id) {
    this.get().setId(id);
  }

  String getName();

  void setName(String name);

  String getUrl();

  void setUrl(String url);

  String getVersion();

  void setVersion(String version);

  List<DependencyInfo> getDependencies();
  Date getApprovalDate();
  ICompositeType getEffectivePeriod();
  List<? extends ICompositeType> getRelatedArtifact();

  IBase accept(KnowledgeArtifactVisitor theVisitor, Repository theRepository, IBaseParameters theParameters);
  String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
  String releaseDescriptionUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
}
