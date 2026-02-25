package org.opencds.cqf.fhir.cr.implementationguide;

import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

/**
 * Result of resolving ImplementationGuide packages.
 * Contains all resources fetched from packages, their source tracking, and dependencies.
 */
public class PackageResolutionResult {
    private final Map<String, IKnowledgeArtifactAdapter> resources;
    private final Map<String, String> resourceSourcePackages;
    private final Map<String, ? extends ICompositeType> allDependencies;

    public PackageResolutionResult(
            Map<String, IKnowledgeArtifactAdapter> resources,
            Map<String, String> resourceSourcePackages,
            Map<String, ? extends ICompositeType> allDependencies) {
        this.resources = new HashMap<>(resources);
        this.resourceSourcePackages = new HashMap<>(resourceSourcePackages);
        this.allDependencies = allDependencies;
    }

    /**
     * Returns all resources fetched from packages.
     * Key: canonical URL (with version if present)
     * Value: resource adapter
     */
    public Map<String, IKnowledgeArtifactAdapter> getResources() {
        return resources;
    }

    /**
     * Returns mapping of resource canonical URLs to their source package canonical URLs.
     * Key: resource canonical URL (with version if present)
     * Value: source package canonical URL
     */
    public Map<String, String> getResourceSourcePackages() {
        return resourceSourcePackages;
    }

    /**
     * Returns all dependencies (relatedArtifacts) collected from packages.
     * Key: dependency canonical URL
     * Value: relatedArtifact composite type
     */
    public Map<String, ? extends ICompositeType> getAllDependencies() {
        return allDependencies;
    }
}
