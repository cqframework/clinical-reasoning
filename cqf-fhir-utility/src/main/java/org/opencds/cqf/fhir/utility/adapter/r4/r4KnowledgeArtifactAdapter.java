package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;

public interface r4KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
  MetadataResource get();
  Period getEffectivePeriod();
  List<RelatedArtifact> getRelatedArtifact();
  default void setName(String name) {
    this.get().setName(name);
  }
  default String getUrl() {
    return this.get().getUrl();
  }
  default String getVersion() {
    return this.get().getVersion();
  }
  default void setVersion(String version) {
    this.get().setVersion(version);
  }
}
