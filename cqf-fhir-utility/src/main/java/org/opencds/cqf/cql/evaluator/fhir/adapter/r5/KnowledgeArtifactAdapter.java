package org.opencds.cqf.cql.evaluator.fhir.adapter.r5;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeArtifactAdapter {
  private MetadataResource myResource;
  KnowledgeArtifactAdapter(MetadataResource theResource) {
    this.myResource = theResource;
  }

  protected List<DependencyInfo> getRelatedArtifactReferences(MetadataResource referencingResource, List<RelatedArtifact> relatedArtifacts) {
    List<DependencyInfo> references = new ArrayList<>();

    for (RelatedArtifact ra : relatedArtifacts) {
      if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF
          || ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON) {
        if (ra.hasResource()) {
          String referenceSource = referencingResource.getUrl();
          if (referencingResource.getVersion() != null && !referencingResource.isEmpty()) {
            referenceSource = referenceSource + "|" + referencingResource.getVersion();
          }

          DependencyInfo dependency = new DependencyInfo(referenceSource, ra.getResourceElement().getValueAsString());
          references.add(dependency);
        }
      }
    }

    return references;
  }
}