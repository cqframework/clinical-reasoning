package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
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
     * @param resolver the conformance resource resolver for looking up base StructureDefinitions
     * @return the list of role codes
     */
    public static List<String> classifyDependencyRoles(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter sourceArtifact,
            IKnowledgeArtifactAdapter dependencyArtifact,
            ConformanceResourceResolver resolver) {

        List<String> roles = new ArrayList<>();

        // Check if this is a key dependency using standards-based analysis
        if (isKeyDependency(dependency, sourceArtifact, dependencyArtifact, resolver)) {
            roles.add("key");
        }

        // All dependencies get the default role (key dependencies can be both key and default)
        roles.add("default");

        return roles;
    }

    /**
     * Classifies the roles for a dependency, also considering transitive key canonicals
     * discovered via ValueSet compose chain walking.
     *
     * @param dependency the dependency information
     * @param sourceArtifact the artifact that has this dependency
     * @param dependencyArtifact the artifact being depended on (may be null if not available)
     * @param resolver the conformance resource resolver
     * @param transitiveKeyCanonicals set of canonical URLs known to be key via compose walking
     * @return the list of role codes
     */
    public static List<String> classifyDependencyRoles(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter sourceArtifact,
            IKnowledgeArtifactAdapter dependencyArtifact,
            ConformanceResourceResolver resolver,
            Set<String> transitiveKeyCanonicals) {

        List<String> roles = new ArrayList<>();

        if (isKeyDependency(dependency, sourceArtifact, dependencyArtifact, resolver)
                || isTransitiveKeyDependency(dependency, transitiveKeyCanonicals)) {
            roles.add("key");
        }

        roles.add("default");
        return roles;
    }

    /**
     * Checks if a dependency is in the set of transitive key canonicals discovered
     * via ValueSet compose chain walking.
     */
    private static boolean isTransitiveKeyDependency(IDependencyInfo dependency, Set<String> transitiveKeyCanonicals) {
        if (transitiveKeyCanonicals == null || transitiveKeyCanonicals.isEmpty()) {
            return false;
        }
        var canonical = getDependencyCanonical(dependency.getReference());
        return canonical != null && transitiveKeyCanonicals.contains(canonical);
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
     * @param resolver the conformance resource resolver
     * @return true if this is a key dependency according to standards-based criteria
     */
    private static boolean isKeyDependency(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter source,
            IKnowledgeArtifactAdapter dependencyArtifact,
            ConformanceResourceResolver resolver) {

        // Only StructureDefinition sources have a formal key element procedure
        if (source != null && source.get().fhirType().equals("StructureDefinition")) {
            return isKeyDependencyForProfile(dependency, dependencyArtifact, source, resolver);
        }

        // For all other artifact types, there is no standards-based definition of "key"
        return false;
    }

    /**
     * Uses KeyElementAnalyzer to determine if a dependency is key for a StructureDefinition.
     * <p>
     * This implements the formal FHIR key element procedure aligned with the IG Publisher's
     * {@code scanForKeyElements()}.
     *
     * @param dependency the dependency information
     * @param dependencyArtifact the resolved dependency artifact (may be null)
     * @param source the StructureDefinition source artifact
     * @param resolver the conformance resource resolver
     * @return true if the dependency is a ValueSet bound to a key element
     */
    private static boolean isKeyDependencyForProfile(
            IDependencyInfo dependency,
            IKnowledgeArtifactAdapter dependencyArtifact,
            IKnowledgeArtifactAdapter source,
            ConformanceResourceResolver resolver) {

        if (resolver == null) {
            return false;
        }

        try {
            // Check if dependency is a ValueSet using the resolved resource type
            if (dependencyArtifact == null
                    || !"ValueSet".equals(dependencyArtifact.get().fhirType())) {
                return false;
            }

            // Use KeyElementAnalyzer to get ValueSets bound to key elements
            FhirVersionEnum fhirVersion = resolver.getFhirVersion();
            KeyElementAnalyzer analyzer = new KeyElementAnalyzer(resolver, fhirVersion);
            IBaseResource structureDefinition = source.get();
            Set<String> keyValueSets = analyzer.getKeyElementValueSets(structureDefinition);

            // Check if this dependency's canonical is in the key ValueSets
            String dependencyCanonical = getDependencyCanonical(dependency.getReference());
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
