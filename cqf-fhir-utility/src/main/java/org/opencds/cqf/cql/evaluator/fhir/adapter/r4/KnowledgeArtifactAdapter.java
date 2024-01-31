package org.opencds.cqf.cql.evaluator.fhir.adapter.r4;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;

public class KnowledgeArtifactAdapter {
    MetadataResource myResource;
  public KnowledgeArtifactAdapter(MetadataResource theResource) {
    this.myResource = theResource;
  }

  protected List<DependencyInfo> getRelatedArtifactReferences(MetadataResource referencingResource, List<RelatedArtifact> relatedArtifacts) {
    List<DependencyInfo> references = new ArrayList<>();
    for (RelatedArtifact ra : relatedArtifacts) {
      if (ra.hasResource()) {
        String referenceSource = referencingResource.getUrl();
        if (referencingResource.getVersion() != null && !referencingResource.isEmpty()) {
          referenceSource = referenceSource + "|" + referencingResource.getVersion();
        }

        DependencyInfo dependency = new DependencyInfo(referenceSource, ra.getResourceElement().getValueAsString());
        references.add(dependency);
      }
    }

    return references;
  }

  public List<RelatedArtifact> getOwnedRelatedArtifacts() {
		return this.getRelatedArtifact().stream()
			.filter(KnowledgeArtifactAdapter::checkIfRelatedArtifactIsOwned)
			.collect(Collectors.toList());
	}
  public static Boolean checkIfRelatedArtifactIsOwned(RelatedArtifact ra){
		return ra.getExtension()
					.stream()
					.filter(ext -> ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/crmi-isOwned"))
					.findAny()
					.map(e -> ((BooleanType) e.getValue()).getValue())
					.orElseGet(()-> false);
	}

  public static Optional<MetadataResource> findLatestVersion(List<MetadataResource> resources) {
		// Comparator<String> versionComparator = SemanticVersion.getVersionComparator();
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
  public void setEffectivePeriod(String effectivePeriod) {
  }
  public List<RelatedArtifact> getRelatedArtifact(){
    return new ArrayList<RelatedArtifact>();
  }
}