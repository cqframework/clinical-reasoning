package org.opencds.cqf.fhir.cr.implementationguide;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.cr.visitor.PackageDownloader;
import org.opencds.cqf.fhir.cr.visitor.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

/**
 * Resolves ImplementationGuide packages and their dependencies.
 * Fetches resources from the package registry and tracks their source packages.
 */
public class ImplementationGuidePackageResolver {
    private final IRepository repository;
    private final FhirContext fhirContext;
    private final IAdapterFactory adapterFactory;
    private PackageDownloader packageDownloader;

    public ImplementationGuidePackageResolver(IRepository repository, FhirContext fhirContext) {
        this.repository = repository;
        this.fhirContext = fhirContext;
        this.adapterFactory = IAdapterFactory.forFhirContext(fhirContext);
    }

    /**
     * Sets the package downloader for testing purposes.
     */
    public void setPackageDownloader(PackageDownloader packageDownloader) {
        this.packageDownloader = packageDownloader;
    }

    /**
     * Resolves packages for an ImplementationGuide.
     *
     * @param igAdapter The ImplementationGuide adapter
     * @param igCanonical The canonical URL of the IG (with version)
     * @param existingDependencies Existing dependencies to check for unresolved IGs
     * @param recursiveGatherCallback Callback to perform recursive gathering on fetched IGs
     * @param artifactVersion List of artifact versions
     * @param checkArtifactVersion List of check artifact versions
     * @param forceArtifactVersion List of force artifact versions
     * @return PackageResolutionResult containing all fetched resources and tracking info
     */
    public PackageResolutionResult resolvePackages(
            IKnowledgeArtifactAdapter igAdapter,
            String igCanonical,
            Map<String, ? extends ICompositeType> existingDependencies,
            RecursiveGatherCallback recursiveGatherCallback,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion) {

        var resources = new HashMap<String, IKnowledgeArtifactAdapter>();
        var resourceSourcePackages = new HashMap<String, String>();
        @SuppressWarnings({"unchecked", "rawtypes"})
        var allDependencies = new HashMap(existingDependencies);

        // Add base FHIR specification package dependencies
        var baseFhirPackages = getBaseFhirPackagesForIG(igAdapter);
        for (String baseFhirPackage : baseFhirPackages) {
            var baseFhirRelatedArtifact = IKnowledgeArtifactAdapter.newRelatedArtifact(
                    fhirContext.getVersion().getVersion(), "depends-on", baseFhirPackage, "Base FHIR Specification");
            allDependencies.put(baseFhirPackage, baseFhirRelatedArtifact);
            IAdapter.logger.info("Added base FHIR specification package as dependency: {}", baseFhirPackage);
        }

        // Fetch main IG package resources
        fetchMainIgPackageResources(igCanonical, resources, resourceSourcePackages);

        // Fetch unresolved ImplementationGuide dependencies from package registry
        fetchImplementationGuideDependencies(
                allDependencies,
                resources,
                resourceSourcePackages,
                recursiveGatherCallback,
                artifactVersion,
                checkArtifactVersion,
                forceArtifactVersion);

        return new PackageResolutionResult(resources, resourceSourcePackages, allDependencies);
    }

    /**
     * Fetches package resources for the main ImplementationGuide.
     */
    private void fetchMainIgPackageResources(
            String igCanonical,
            Map<String, IKnowledgeArtifactAdapter> resources,
            Map<String, String> resourceSourcePackages) {
        IAdapter.logger.info("Fetching package resources for main IG: {}", igCanonical);
        var mainIgPackageResources = packageDownloader != null
                ? PackageHelper.fetchPackageResources(
                        igCanonical, fhirContext, adapterFactory, repository, packageDownloader)
                : PackageHelper.fetchPackageResources(igCanonical, fhirContext, adapterFactory, repository);

        // Add main IG package resources to gathered resources and track source package
        mainIgPackageResources.forEach((resourceCanonicalUrl, resourceAdapter) -> {
            if (!resources.containsKey(resourceCanonicalUrl)) {
                resources.put(resourceCanonicalUrl, resourceAdapter);
                resourceSourcePackages.put(resourceCanonicalUrl, igCanonical);
                IAdapter.logger.debug(
                        "Added {} from main IG package to gathered resources",
                        resourceAdapter.get().fhirType());
            }
        });
        IAdapter.logger.info(
                "Added {} resources from main IG package. Total gathered resources: {}",
                mainIgPackageResources.size(),
                resources.size());
    }

    /**
     * PHASE 1: Fetches unresolved ImplementationGuide dependencies from the package registry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void fetchImplementationGuideDependencies(
            Map allDependencies,
            Map<String, IKnowledgeArtifactAdapter> resources,
            Map<String, String> resourceSourcePackages,
            RecursiveGatherCallback recursiveGatherCallback,
            List<String> artifactVersion,
            List<String> checkArtifactVersion,
            List<String> forceArtifactVersion) {

        IAdapter.logger.info("Phase 1: Fetching unresolved ImplementationGuide dependencies from package registry");

        var newRelatedArtifacts = new HashMap<String, ICompositeType>();

        allDependencies.forEach((canonical, ra) -> {
            @SuppressWarnings("unchecked")
            var relArt = (ICompositeType) ra;
            String canonicalStr = (String) canonical;
            boolean wasResolved = resources.values().stream().anyMatch(r -> {
                String resourceCanonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                return canonicalStr.equals(resourceCanonical) || canonicalStr.startsWith(r.getUrl());
            });

            if (!wasResolved) {
                String relType = getRelatedArtifactTypeUnchecked(relArt);
                boolean isIgDependency = "depends-on".equals(relType) && canonicalStr.contains("ImplementationGuide");

                if (isIgDependency) {
                    IAdapter.logger.info(
                            "Attempting to fetch unresolved ImplementationGuide from package registry: {}",
                            canonicalStr);
                    var igResource = PackageHelper.fetchImplementationGuideFromRegistry(canonicalStr, fhirContext);

                    IKnowledgeArtifactAdapter igAdapter = null;
                    String igCanonical = null;

                    if (igResource != null) {
                        igAdapter = adapterFactory.createKnowledgeArtifactAdapter(igResource);
                        IAdapter.logger.info(
                                "Successfully fetched IG from registry, now fetching all package resources: {}",
                                canonicalStr);
                        igCanonical = igAdapter.hasVersion()
                                ? igAdapter.getUrl() + "|" + igAdapter.getVersion()
                                : igAdapter.getUrl();
                        resources.put(igCanonical, igAdapter);
                    } else {
                        IAdapter.logger.info(
                                "No ImplementationGuide found in package (expected for terminology packages like VSAC/PHINVADS), "
                                        + "but will still fetch package resources: {}",
                                canonicalStr);
                    }

                    final String sourcePackageCanonical = igCanonical != null ? igCanonical : canonicalStr;

                    // Fetch ALL resources from the package
                    var packageResources = packageDownloader != null
                            ? PackageHelper.fetchPackageResources(
                                    canonicalStr, fhirContext, adapterFactory, repository, packageDownloader)
                            : PackageHelper.fetchPackageResources(
                                    canonicalStr, fhirContext, adapterFactory, repository);

                    // Add all package resources to gathered resources and track source package
                    packageResources.forEach((resourceCanonicalUrl, resourceAdapter) -> {
                        if (!resources.containsKey(resourceCanonicalUrl)) {
                            resources.put(resourceCanonicalUrl, resourceAdapter);
                            resourceSourcePackages.put(resourceCanonicalUrl, sourcePackageCanonical);
                            IAdapter.logger.debug(
                                    "Added {} from package {} to gathered resources",
                                    resourceAdapter.get().fhirType(),
                                    canonicalStr);
                        }
                    });

                    IAdapter.logger.info(
                            "Added {} resources from package {}. Total gathered resources now: {}",
                            packageResources.size(),
                            canonicalStr,
                            resources.size());

                    // If we have an IG, collect its relatedArtifacts and process recursively
                    if (igAdapter != null) {
                        igAdapter.getRelatedArtifact().stream()
                                .filter(igRa -> IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa) != null)
                                .forEach(igRa -> {
                                    String raCanonical = IKnowledgeArtifactAdapter.getRelatedArtifactReference(igRa);
                                    if (!allDependencies.containsKey(raCanonical)
                                            && !newRelatedArtifacts.containsKey(raCanonical)) {
                                        newRelatedArtifacts.put(raCanonical, (ICompositeType) igRa);
                                        IAdapter.logger.debug(
                                                "Collected relatedArtifact from fetched IG: {}", raCanonical);
                                    }
                                });

                        if (recursiveGatherCallback != null) {
                            recursiveGatherCallback.gatherDependencies(
                                    igAdapter,
                                    resources,
                                    forceArtifactVersion,
                                    forceArtifactVersion,
                                    new ImmutableTriple<>(artifactVersion, checkArtifactVersion, forceArtifactVersion));
                        }

                        IAdapter.logger.info(
                                "Completed gathering resources from fetched IG: {}. Total gathered resources now: {}",
                                canonicalStr,
                                resources.size());
                    }
                }
            }
        });

        // Add collected relatedArtifacts from fetched IGs to allDependencies
        if (!newRelatedArtifacts.isEmpty()) {
            newRelatedArtifacts.forEach((canonical, ra) -> allDependencies.put(canonical, ra));
            IAdapter.logger.info(
                    "Added {} relatedArtifacts from fetched IGs to allDependencies. Total: {}",
                    newRelatedArtifacts.size(),
                    allDependencies.size());
        }
    }

    /**
     * Gets the base FHIR specification package canonicals for an ImplementationGuide.
     */
    private List<String> getBaseFhirPackagesForIG(IKnowledgeArtifactAdapter igAdapter) {
        var baseFhirPackages = new ArrayList<String>();

        // Access fhirVersion field based on FHIR version
        switch (fhirContext.getVersion().getVersion()) {
            case DSTU3 -> {
                var ig = (org.hl7.fhir.dstu3.model.ImplementationGuide) igAdapter.get();
                if (ig.hasFhirVersion()) {
                    String fhirVersion = ig.getFhirVersion();
                    baseFhirPackages.add(mapFhirVersionToPackageCanonical(fhirVersion));
                }
            }
            case R4 -> {
                var ig = (org.hl7.fhir.r4.model.ImplementationGuide) igAdapter.get();
                if (ig.hasFhirVersion()) {
                    for (var fhirVersionEnum : ig.getFhirVersion()) {
                        String fhirVersion = fhirVersionEnum.asStringValue();
                        baseFhirPackages.add(mapFhirVersionToPackageCanonical(fhirVersion));
                    }
                }
            }
            case R5 -> {
                var ig = (org.hl7.fhir.r5.model.ImplementationGuide) igAdapter.get();
                if (ig.hasFhirVersion()) {
                    for (var fhirVersionEnum : ig.getFhirVersion()) {
                        String fhirVersion = fhirVersionEnum.asStringValue();
                        baseFhirPackages.add(mapFhirVersionToPackageCanonical(fhirVersion));
                    }
                }
            }
            default -> IAdapter.logger.warn(
                    "Unsupported FHIR version for reading ImplementationGuide.fhirVersion: {}",
                    fhirContext.getVersion().getVersion());
        }

        return baseFhirPackages;
    }

    /**
     * Maps a FHIR version string to the corresponding base FHIR specification package canonical.
     */
    private String mapFhirVersionToPackageCanonical(String fhirVersion) {
        // Determine the package ID based on major.minor version
        String packageId;
        if (fhirVersion.startsWith("4.0")) {
            packageId = "hl7.fhir.r4.core";
        } else if (fhirVersion.startsWith("3.0")) {
            packageId = "hl7.fhir.r3.core";
        } else if (fhirVersion.startsWith("5.0")) {
            packageId = "hl7.fhir.r5.core";
        } else if (fhirVersion.startsWith("4.3")) {
            // R4B
            packageId = "hl7.fhir.r4b.core";
        } else if (fhirVersion.startsWith("1.0")) {
            // DSTU2
            packageId = "hl7.fhir.r2.core";
        } else {
            IAdapter.logger.warn("Unknown FHIR version for base package mapping: {}", fhirVersion);
            // Default to using the version as-is and hope the package exists
            packageId = "hl7.fhir.core";
        }

        // Return canonical URL in packageId|version format
        return String.format("http://hl7.org/fhir/ImplementationGuide/%s|%s", packageId, fhirVersion);
    }

    /**
     * Helper method to call getRelatedArtifactType with proper unchecked suppression.
     * This is needed when working with raw types.
     */
    @SuppressWarnings("unchecked")
    private <T extends ICompositeType & org.hl7.fhir.instance.model.api.IBaseHasExtensions>
            String getRelatedArtifactTypeUnchecked(Object ra) {
        return IKnowledgeArtifactAdapter.getRelatedArtifactType((T) ra);
    }

    /**
     * Callback interface for recursive gathering of dependencies.
     */
    @FunctionalInterface
    public interface RecursiveGatherCallback {
        void gatherDependencies(
                IKnowledgeArtifactAdapter adapter,
                Map<String, IKnowledgeArtifactAdapter> gatheredResources,
                List<String> artifactVersion,
                List<String> checkArtifactVersion,
                ImmutableTriple<List<String>, List<String>, List<String>> versionTriple);
    }
}
