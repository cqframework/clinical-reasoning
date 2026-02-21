package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for resolving which package a dependency comes from.
 * <p>
 * Uses a multi-strategy approach:
 * <ol>
 *   <li>Check if artifact has package-source extension</li>
 *   <li>Search for ImplementationGuide that defines this artifact</li>
 *   <li>Look for package metadata in artifact itself</li>
 * </ol>
 */
public class PackageSourceResolver {
    private static final Logger logger = LoggerFactory.getLogger(PackageSourceResolver.class);

    private PackageSourceResolver() {}

    /**
     * Resolves the package source (packageId#version) for a dependency artifact.
     *
     * @param artifact the dependency artifact
     * @param repository the repository for searching
     * @return Optional containing the package source string (e.g., "hl7.fhir.us.core#6.1.0"), or empty if not found
     */
    public static Optional<String> resolvePackageSource(IKnowledgeArtifactAdapter artifact, IRepository repository) {
        if (artifact == null) {
            return Optional.empty();
        }

        // Strategy 1: Check if artifact has package-source extension
        Optional<String> fromExtension = extractFromPackageSourceExtension(artifact);
        if (fromExtension.isPresent()) {
            return fromExtension;
        }

        // Strategy 2: Search for ImplementationGuide that defines this artifact
        Optional<String> fromIgSearch = searchImplementationGuideForArtifact(artifact, repository);
        if (fromIgSearch.isPresent()) {
            return fromIgSearch;
        }

        // Strategy 3: Look for package metadata in artifact itself
        // (This could be expanded in the future if there are other ways artifacts carry package info)

        return Optional.empty();
    }

    /**
     * Extracts package source from the artifact's package-source extension if present.
     *
     * @param artifact the artifact to check
     * @return Optional containing the package source, or empty if not found
     */
    private static Optional<String> extractFromPackageSourceExtension(IKnowledgeArtifactAdapter artifact) {
        try {
            // Try using the adapter's getExtensionByUrl method
            List<? extends IBaseExtension<?, ?>> extensions = artifact.getExtensionByUrl(Constants.PACKAGE_SOURCE);
            if (extensions != null && !extensions.isEmpty()) {
                IBaseExtension<?, ?> ext = extensions.get(0);
                Object value = ext.getValue();
                if (value instanceof IPrimitiveType<?> primitiveValue) {
                    String packageSource = primitiveValue.getValueAsString();
                    if (packageSource != null && !packageSource.isEmpty()) {
                        return Optional.of(packageSource);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting package-source extension via adapter", e);
        }

        // Fallback: Try getting extensions directly from the resource
        try {
            var resource = artifact.get();
            if (resource instanceof IBaseHasExtensions) {
                IBaseHasExtensions hasExtensions = (IBaseHasExtensions) resource;
                for (var ext : hasExtensions.getExtension()) {
                    if (Constants.PACKAGE_SOURCE.equals(ext.getUrl())) {
                        Object value = ext.getValue();
                        if (value instanceof IPrimitiveType<?>) {
                            IPrimitiveType<?> primitiveValue = (IPrimitiveType<?>) value;
                            String packageSource = primitiveValue.getValueAsString();
                            if (packageSource != null && !packageSource.isEmpty()) {
                                return Optional.of(packageSource);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting package-source extension from resource", e);
        }

        return Optional.empty();
    }

    /**
     * Searches for an ImplementationGuide resource that references this artifact.
     *
     * @param artifact the artifact to search for
     * @param repository the repository to search in
     * @return Optional containing the package source, or empty if not found
     */
    private static Optional<String> searchImplementationGuideForArtifact(
            IKnowledgeArtifactAdapter artifact, IRepository repository) {
        if (repository == null) {
            return Optional.empty();
        }

        try {
            var artifactUrl = artifact.getUrl();
            if (artifactUrl == null || artifactUrl.isEmpty()) {
                return Optional.empty();
            }

            // Search for ImplementationGuide resources (version-specific)
            var fhirVersion = repository.fhirContext().getVersion().getVersion();
            var igBundle = searchForImplementationGuides(repository, fhirVersion);

            if (igBundle == null) {
                return Optional.empty();
            }

            // Check each IG to see if it references this artifact
            for (var entry : BundleHelper.getEntry(igBundle)) {
                var resource = BundleHelper.getEntryResource(fhirVersion, entry);

                if ("ImplementationGuide".equals(resource.fhirType())) {
                    Optional<String> packageSource = checkIfIgDefinesArtifact(resource, artifactUrl, repository);
                    if (packageSource.isPresent()) {
                        return packageSource;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error searching for ImplementationGuide", e);
        }

        return Optional.empty();
    }

    /**
     * Searches for ImplementationGuide resources based on FHIR version.
     *
     * @param repository the repository to search
     * @param fhirVersion the FHIR version
     * @return Bundle containing Implementation Guides
     */
    private static org.hl7.fhir.instance.model.api.IBaseBundle searchForImplementationGuides(
            IRepository repository, FhirVersionEnum fhirVersion) {
        Map<String, List<IQueryParameterType>> emptySearchParams = Collections.emptyMap();
        Map<String, String> emptyHeaders = Collections.emptyMap();

        return switch (fhirVersion) {
            case DSTU3 ->
                SearchHelper.searchRepositoryWithPaging(
                        repository,
                        org.hl7.fhir.dstu3.model.ImplementationGuide.class,
                        emptySearchParams,
                        emptyHeaders);
            case R4 ->
                SearchHelper.searchRepositoryWithPaging(
                        repository, org.hl7.fhir.r4.model.ImplementationGuide.class, emptySearchParams, emptyHeaders);
            case R5 ->
                SearchHelper.searchRepositoryWithPaging(
                        repository, org.hl7.fhir.r5.model.ImplementationGuide.class, emptySearchParams, emptyHeaders);
            default -> null;
        };
    }

    /**
     * Checks if an ImplementationGuide defines the given artifact.
     *
     * @param igResource the ImplementationGuide resource
     * @param artifactUrl the canonical URL of the artifact
     * @param repository the repository
     * @return Optional containing the package source if the IG defines this artifact
     */
    private static Optional<String> checkIfIgDefinesArtifact(
            org.hl7.fhir.instance.model.api.IBaseResource igResource, String artifactUrl, IRepository repository) {
        try {
            var fhirVersion = repository.fhirContext().getVersion().getVersion();
            var adapterFactory = org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.forFhirVersion(fhirVersion);
            var igAdapter = adapterFactory.createImplementationGuide(
                    (org.hl7.fhir.instance.model.api.IDomainResource) igResource);

            // Get all resources defined in this IG
            var dependencies = igAdapter.getDependencies(repository);

            // Check if any of the IG's resources match our artifact
            for (var dep : dependencies) {
                String depUrl = dep.getReference();
                // Strip version from both URLs for comparison
                String depUrlNoVersion = depUrl.contains("|") ? depUrl.substring(0, depUrl.indexOf('|')) : depUrl;
                String artifactUrlNoVersion =
                        artifactUrl.contains("|") ? artifactUrl.substring(0, artifactUrl.indexOf('|')) : artifactUrl;

                if (depUrlNoVersion.equals(artifactUrlNoVersion)) {
                    // Found! Extract package ID and version from IG
                    String packageId = extractPackageId(igAdapter);
                    String version = igAdapter.getVersion();
                    return Optional.of(formatPackageSource(packageId, version));
                }
            }
        } catch (Exception e) {
            logger.debug("Error checking ImplementationGuide for artifact", e);
        }

        return Optional.empty();
    }

    /**
     * Extracts a package ID from an ImplementationGuide.
     * Tries: packageId, name, id (in that order).
     *
     * @param igAdapter the ImplementationGuide adapter
     * @return the package ID
     */
    private static String extractPackageId(org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter igAdapter) {
        // Strategy 1: Check for packageId property (R4+)
        try {
            var ig = igAdapter.get();
            if (ig instanceof org.hl7.fhir.r4.model.ImplementationGuide r4Ig && r4Ig.hasPackageId()) {
                return r4Ig.getPackageId();
            } else if (ig instanceof org.hl7.fhir.r5.model.ImplementationGuide r5Ig && r5Ig.hasPackageId()) {
                return r5Ig.getPackageId();
            }
        } catch (Exception e) {
            logger.debug("Error extracting packageId property", e);
        }

        // Strategy 2: Use name
        String name = igAdapter.getName();
        if (name != null && !name.isEmpty()) {
            return name;
        }

        // Strategy 3: Use id as last resort
        var idType = igAdapter.getId();
        if (idType != null) {
            String id = idType.getValueAsString();
            if (id != null && !id.isEmpty()) {
                return id;
            }
        }

        return "unknown";
    }

    /**
     * Formats the package source string from packageId and version.
     *
     * @param packageId the package ID
     * @param version the version (may be null)
     * @return the formatted package source string
     */
    public static String formatPackageSource(String packageId, String version) {
        if (version != null && !version.isEmpty()) {
            return packageId + "#" + version;
        }
        return packageId;
    }
}
