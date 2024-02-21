package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import org.opencds.cqf.fhir.utility.visitor.r4.r4KnowledgeArtifactVisitor;

public interface r4KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
  MetadataResource get();
  Period getEffectivePeriod();
  void setEffectivePeriod(Period effectivePeriod);
  List<RelatedArtifact> getRelatedArtifact();
  void setRelatedArtifact(List<RelatedArtifact> relatedArtifacts);
  default void setName(String name) {
    this.get().setName(name);
  }
  default String getName() {
    return this.get().getName();
  }
  default String getUrl() {
    return this.get().getUrl();
  }
  default void setUrl(String url) {
    this.get().setUrl(url);
  }
  default String getVersion() {
    return this.get().getVersion();
  }
  default void setVersion(String version) {
    this.get().setVersion(version);
  }
  default List<RelatedArtifact> getOwnedRelatedArtifacts() {
		return this.getRelatedArtifact().stream()
			.filter(r4KnowledgeArtifactAdapter::checkIfRelatedArtifactIsOwned)
			.collect(Collectors.toList());
  }
  static Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra){
		return ra.getExtension()
            .stream()
            .filter(ext -> ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/crmi-isOwned"))
            .findAny()
            .map(e -> ((BooleanType) e.getValue()).getValue())
            .orElseGet(()-> false);
  }
  default List<RelatedArtifact> getComponents() {
    return this.getRelatedArtifactsOfType(RelatedArtifact.RelatedArtifactType.COMPOSEDOF);
  }
  default List<RelatedArtifact> getRelatedArtifactsOfType(RelatedArtifact.RelatedArtifactType relatedArtifactType) {
    List<RelatedArtifact> relatedArtifacts = getRelatedArtifact().stream()
        .filter(ra -> ra.getType() == relatedArtifactType)
        .collect(Collectors.toList());
    return relatedArtifacts;
  }
  default List<DependencyInfo> combineComponentsAndDependencies() {
		return Stream.concat(getComponents().stream().map(ra -> DependencyInfo.convertRelatedArtifact(ra, getUrl() + "|" + getVersion())), getDependencies().stream()).collect(Collectors.toList());
  }
}
