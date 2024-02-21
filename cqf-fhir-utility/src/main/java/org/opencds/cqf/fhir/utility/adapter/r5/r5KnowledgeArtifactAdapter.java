package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;

public interface r5KnowledgeArtifactAdapter extends IBaseKnowledgeArtifactAdapter {
    MetadataResource get();

    Period getEffectivePeriod();

    List<RelatedArtifact> getRelatedArtifact();

    void setEffectivePeriod(Period effectivePeriod);

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
                .filter(r5KnowledgeArtifactAdapter::checkIfRelatedArtifactIsOwned)
                .collect(Collectors.toList());
    }

    static Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra) {
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
