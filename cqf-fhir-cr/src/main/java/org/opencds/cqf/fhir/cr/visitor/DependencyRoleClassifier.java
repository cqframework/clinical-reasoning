package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for classifying dependency roles based on standards-based analysis.
 * <p>
 * Roles can include:
 * <ul>
 *   <li><b>key</b>: Required to implement or interpret key elements of the artifact</li>
 *   <li><b>default</b>: General supporting dependency not directly tied to key elements</li>
 * </ul>
 * <p>
 * For StructureDefinition sources, uses {@link KeyElementAnalyzer} to determine
 * if ValueSets are bound to key elements according to the formal FHIR procedure.
 * <p>
 * For all other artifact types, dependencies are classified as "default" unless
 * standards-based criteria identify them as "key".
 */
public class DependencyRoleClassifier {
    private static final Logger logger = LoggerFactory.getLogger(DependencyRoleClassifier.class);

    private DependencyRoleClassifier() {}

    /**
     * Classifies the roles for a dependency using standards-based analysis.
     *
     * @param dependency the dependency information
     * @param sourceArtifact the artifact that has this dependency
     * @param dependencyArtifact the artifact being depended on (may be null if not available)
     * @param repository the repository for looking up additional information
     * @return the list of role codes
     */
    public static List<String> classifyDependencyRoles(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter sourceArtifact,
            IKnowledgeArtifactAdapter dependencyArtifact,
            IRepository repository) {

        List<String> roles = new ArrayList<>();

        // Check if this is a key dependency using standards-based analysis
        if (isKeyDependency(dependency, sourceArtifact, dependencyArtifact, repository)) {
            roles.add("key");
        }

        // All dependencies get the default role (key dependencies can be both key and default)
        roles.add("default");

        return roles;
    }

    /**
     * Determines if a dependency is a key dependency using standards-based analysis.
     * <p>
     * For StructureDefinition (profile) sources with ValueSet dependencies, this uses the
     * formal key element procedure defined in FHIR to determine if the ValueSet is bound
     * to key elements.
     * <p>
     * For other artifact types, there is currently no formal FHIR specification that defines
     * "key" dependencies, so they are not classified as key.
     *
     * @param dependency the dependency information
     * @param source the source artifact
     * @param dependencyArtifact the dependency artifact (may be null)
     * @param repository the repository for resolving profiles
     * @return true if this is a key dependency according to standards-based criteria
     */
    private static boolean isKeyDependency(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter source,
            IKnowledgeArtifactAdapter dependencyArtifact,
            IRepository repository) {

        // Only StructureDefinition sources have a formal key element procedure
        if (source != null && source.get().fhirType().equals("StructureDefinition")) {
            return isKeyDependencyForProfile(dependency, source, repository);
        }

        // For all other artifact types, there is no standards-based definition of "key"
        return false;
    }

    /**
     * Uses KeyElementAnalyzer to determine if a dependency is key for a StructureDefinition.
     * <p>
     * This implements the formal FHIR key element procedure:
     * <ul>
     *   <li>Step A: Build seed set (mustSupport, differential, ancestors)</li>
     *   <li>Step B: Expand downward (mandatory children, constrained, slices, modifiers)</li>
     *   <li>Step C: Extract ValueSet bindings from key elements</li>
     *   <li>Step D: Walk inheritance chain</li>
     * </ul>
     *
     * @param dependency the dependency information
     * @param source the StructureDefinition source artifact
     * @param repository the repository for resolving base definitions
     * @return true if the dependency is a ValueSet bound to a key element
     */
    private static boolean isKeyDependencyForProfile(
            IDependencyInfo dependency, IKnowledgeArtifactAdapter source, IRepository repository) {

        if (repository == null) {
            return false;
        }

        try {
            // Check if dependency is a ValueSet
            String dependencyRef = dependency.getReference();
            if (dependencyRef == null || !dependencyRef.contains("ValueSet")) {
                return false;
            }

            // Use KeyElementAnalyzer to get ValueSets bound to key elements
            KeyElementAnalyzer analyzer = new KeyElementAnalyzer(repository);
            IBaseResource structureDefinition = source.get();
            Set<String> keyValueSets = analyzer.getKeyElementValueSets(structureDefinition);

            // Check if this dependency's canonical is in the key ValueSets
            String dependencyCanonical = getDependencyCanonical(dependencyRef);
            for (String keyVs : keyValueSets) {
                if (canonicalsMatch(keyVs, dependencyCanonical)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.debug("Error analyzing key elements for profile", e);
        }

        return false;
    }

    /**
     * Extracts the canonical URL from a dependency reference.
     */
    private static String getDependencyCanonical(String reference) {
        if (reference == null) {
            return null;
        }
        // Remove version if present (url|version -> url)
        int pipeIndex = reference.indexOf('|');
        return pipeIndex > 0 ? reference.substring(0, pipeIndex) : reference;
    }

    /**
     * Checks if two canonical URLs match (ignoring versions).
     */
    private static boolean canonicalsMatch(String canonical1, String canonical2) {
        if (canonical1 == null || canonical2 == null) {
            return false;
        }

        String url1 = getDependencyCanonical(canonical1);
        String url2 = getDependencyCanonical(canonical2);

        return url1 != null && url1.equals(url2);
    }
}
