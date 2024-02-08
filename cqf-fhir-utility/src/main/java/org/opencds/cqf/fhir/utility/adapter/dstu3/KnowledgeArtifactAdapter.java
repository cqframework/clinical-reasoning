package org.opencds.cqf.fhir.utility.adapter.dstu3;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import java.util.ArrayList;
import java.util.List;

public class KnowledgeArtifactAdapter extends ResourceAdapter {
    private MetadataResource myResource;
  KnowledgeArtifactAdapter(MetadataResource theResource) {
    super(theResource);
    this.myResource = theResource;
  }
  protected MetadataResource getResource() {
    return this.myResource;
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

          DependencyInfo dependency = new DependencyInfo(referenceSource, ra.getResource().getReference(), ra.getExtension());
          references.add(dependency);
        }
      }
    }

    return references;
  }
  public static Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra){
    return ra.getExtension()
        .stream()
        .filter(ext -> ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/crmi-isOwned"))
        .findAny()
        .map(e -> ((BooleanType) e.getValue()).getValue())
        .orElseGet(()-> false);
}
}