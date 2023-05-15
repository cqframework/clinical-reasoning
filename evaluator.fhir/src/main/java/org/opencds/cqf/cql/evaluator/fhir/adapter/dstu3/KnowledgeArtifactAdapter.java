package org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeArtifactAdapter extends org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.ResourceAdapter {

  KnowledgeArtifactAdapter(IBaseResource resource) {
    super(resource);
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

          DependencyInfo dependency = new DependencyInfo(referenceSource, ra.getResource().getReference());
          references.add(dependency);
        }
      }
    }

    return references;
  }
}