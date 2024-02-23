package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class KnowledgeArtifactAdapter extends ResourceAdapter implements r4KnowledgeArtifactAdapter {
    MetadataResource adaptedResource;

    public KnowledgeArtifactAdapter(MetadataResource resource) {
        super(resource);
        this.adaptedResource = resource;
    }

    public r4KnowledgeArtifactAdapter adapt(Library library) {
        return new LibraryAdapter(library);
    }

    public r4KnowledgeArtifactAdapter adapt(PlanDefinition library) {
        return new PlanDefinitionAdapter(library);
    }

    @Override
    public MetadataResource get() {
        return this.adaptedResource;
    }

    @Override
    public Date getApprovalDate() {
        return null;
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        // do nothing
    }

    @Override
    public Period getEffectivePeriod() {
        return new Period();
    }

    @Override
    public List<DependencyInfo> getDependencies() {
        return new ArrayList<>();
    }

    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    protected List<DependencyInfo> getRelatedArtifactReferences(
            MetadataResource referencingResource, List<RelatedArtifact> relatedArtifacts) {
        List<DependencyInfo> references = new ArrayList<>();
        for (RelatedArtifact ra : relatedArtifacts) {
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

        return references;
    }

    public static Optional<MetadataResource> findLatestVersion(List<MetadataResource> resources) {
        Comparator<String> versionComparator = (String v1, String v2) -> v1.compareTo(v2);
        MetadataResource latestResource = null;

        for (MetadataResource resource : resources) {
            String version = resource.getVersion();
            if (latestResource == null || versionComparator.compare(version, latestResource.getVersion()) > 0) {
                latestResource = resource;
            }
        }

        return Optional.ofNullable(latestResource);
    }

    public static Optional<MetadataResource> findLatestVersion(Bundle bundle) {
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        List<MetadataResource> metadataResources = new ArrayList<>();

        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof MetadataResource) {
                MetadataResource metadataResource = (MetadataResource) resource;
                metadataResources.add(metadataResource);
            }
        }

        return findLatestVersion(metadataResources);
    }

    public void setEffectivePeriod(Period effectivePeriod) {
        // does nothing
    }

    public List<RelatedArtifact> getRelatedArtifact() {
        return new ArrayList<RelatedArtifact>();
    }

    public void setRelatedArtifact(List<RelatedArtifact> relatedArtifacts) {
        // does nothing
    }
}
