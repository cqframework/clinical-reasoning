package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Constants;
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
            logger.debug("Error extracting package-source extension", e);
        }
        return Optional.empty();
    }

    /**
     * Searches for an ImplementationGuide resource that references this artifact.
     * TODO: Implement IG search when repository API is stable.
     *
     * @param artifact the artifact to search for
     * @param repository the repository to search in
     * @return Optional containing the package source, or empty if not found
     */
    private static Optional<String> searchImplementationGuideForArtifact(
            IKnowledgeArtifactAdapter artifact, IRepository repository) {
        // Placeholder for future implementation
        // Would search for ImplementationGuide resources that reference this artifact
        return Optional.empty();
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
