package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class KnowledgeArtifactAdapter extends ResourceAdapter {
    private MetadataResource adaptedResource;

    KnowledgeArtifactAdapter(MetadataResource resource) {
        super(resource);
        this.adaptedResource = resource;
    }

    protected MetadataResource getResource() {
        return this.adaptedResource;
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

                    DependencyInfo dependency =
                            new DependencyInfo(referenceSource, ra.getResource().getReference(), ra.getExtension());
                    references.add(dependency);
                }
            }
        }

        return references;
    }
}
