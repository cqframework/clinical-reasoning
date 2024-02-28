package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class KnowledgeArtifactAdapter extends ResourceAdapter {
    private MetadataResource adaptedResource;

    KnowledgeArtifactAdapter(MetadataResource resource) {
        super(resource);
        this.adaptedResource = resource;
    }

    protected List<IDependencyInfo> getRelatedArtifactReferences(
            MetadataResource referencingResource, List<RelatedArtifact> relatedArtifacts) {
        List<IDependencyInfo> references = new ArrayList<>();

        for (RelatedArtifact ra : relatedArtifacts) {
            if (ra.getType() == RelatedArtifact.RelatedArtifactType.COMPOSEDOF
                    || ra.getType() == RelatedArtifact.RelatedArtifactType.DEPENDSON) {
                if (ra.hasResource()) {
                    String referenceSource = referencingResource.getUrl();
                    if (referencingResource.getVersion() != null && !referencingResource.isEmpty()) {
                        referenceSource = referenceSource + "|" + referencingResource.getVersion();
                    }

                    DependencyInfo dependency = new DependencyInfo(
                            referenceSource, ra.getResourceElement().getValueAsString(), ra.getExtension());
                    references.add(dependency);
                }
            }
        }

        return references;
    }
}
