package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;

public interface Dstu3KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
    MetadataResource get();

    Period getEffectivePeriod();

    List<RelatedArtifact> getRelatedArtifact();

    default List<RelatedArtifact> getOwnedRelatedArtifacts() {
        return this.getRelatedArtifact().stream()
                .filter(this::checkIfRelatedArtifactIsOwned)
                .collect(Collectors.toList());
    }

    default Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra) {
        return ra.getExtension().stream()
                .filter(ext -> ext.getUrl().equals(isOwnedUrl))
                .findAny()
                .map(e -> ((BooleanType) e.getValue()).getValue())
                .orElseGet(() -> false);
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
}
