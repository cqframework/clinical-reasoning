package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Walks ValueSet compose chains to discover transitive CodeSystem and ValueSet dependencies.
 * <p>
 * Given a set of key ValueSet canonical URLs (typically from {@link KeyElementAnalyzer}), this walker
 * resolves each ValueSet from the package cache and extracts:
 * <ul>
 *   <li>{@code compose.include[].system} → CodeSystem dependencies</li>
 *   <li>{@code compose.include[].valueSet} → transitive ValueSet dependencies (walked recursively)</li>
 * </ul>
 * <p>
 * The compose is structural metadata on the ValueSet resource itself — no terminology server
 * expansion is required. All resources are resolved from NPM packages via
 * {@link ConformanceResourceResolver}.
 */
public class ValueSetComposeWalker {
    private static final Logger logger = LoggerFactory.getLogger(ValueSetComposeWalker.class);

    private final ConformanceResourceResolver resolver;
    private final FhirVersionEnum fhirVersion;

    public ValueSetComposeWalker(ConformanceResourceResolver resolver, FhirVersionEnum fhirVersion) {
        this.resolver = resolver;
        this.fhirVersion = fhirVersion;
    }

    /**
     * Walks the compose chains of the given key ValueSets to discover transitive dependencies.
     *
     * @param keyValueSetUrls canonical URLs of ValueSets classified as key (may include versions)
     * @return result containing all discovered transitive key CodeSystems and ValueSets
     */
    public ComposeWalkResult walkComposeChains(Set<String> keyValueSetUrls) {
        var transitiveCodeSystems = new HashSet<String>();
        var transitiveValueSets = new HashSet<String>();
        var visited = new HashSet<String>();
        var queue = new ArrayDeque<String>();

        // Seed the queue with key ValueSet URLs (strip versions for visited tracking)
        for (var url : keyValueSetUrls) {
            var baseUrl = stripVersion(url);
            if (visited.add(baseUrl)) {
                queue.add(url);
            }
        }

        while (!queue.isEmpty()) {
            var valueSetUrl = queue.poll();
            var resolved = resolver.resolveResource(stripVersion(valueSetUrl), "ValueSet");
            if (resolved == null) {
                logger.debug("Could not resolve ValueSet for compose walking: {}", valueSetUrl);
                continue;
            }

            try {
                var adapter = createValueSetAdapter(resolved);
                if (adapter == null) {
                    continue;
                }

                // Extract CodeSystems from compose.include[].system
                for (var include : adapter.getComposeInclude()) {
                    var system = include.getSystem();
                    if (system != null && !system.isEmpty()) {
                        transitiveCodeSystems.add(system);
                    }
                }

                // Extract and enqueue transitive ValueSets from compose.include[].valueSet
                for (var includedVsUrl : adapter.getValueSetIncludes()) {
                    if (includedVsUrl != null && !includedVsUrl.isEmpty()) {
                        transitiveValueSets.add(stripVersion(includedVsUrl));
                        var baseUrl = stripVersion(includedVsUrl);
                        if (visited.add(baseUrl)) {
                            queue.add(includedVsUrl);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Error walking compose for ValueSet {}: {}", valueSetUrl, e.getMessage());
            }
        }

        logger.debug(
                "Compose walk complete: {} transitive CodeSystems, {} transitive ValueSets",
                transitiveCodeSystems.size(),
                transitiveValueSets.size());

        return new ComposeWalkResult(transitiveValueSets, transitiveCodeSystems);
    }

    private IValueSetAdapter createValueSetAdapter(IBaseResource resource) {
        try {
            return IAdapterFactory.forFhirVersion(fhirVersion).createValueSet((IDomainResource) resource);
        } catch (Exception e) {
            logger.debug("Could not create ValueSet adapter: {}", e.getMessage());
            return null;
        }
    }

    private static String stripVersion(String canonical) {
        var version = Canonicals.getVersion(canonical);
        return version != null ? Canonicals.getUrl(canonical) : canonical;
    }

    /**
     * Result of walking ValueSet compose chains.
     *
     * @param transitiveValueSets ValueSet canonical URLs discovered via compose.include[].valueSet
     * @param transitiveCodeSystems CodeSystem URLs discovered via compose.include[].system
     */
    public record ComposeWalkResult(Set<String> transitiveValueSets, Set<String> transitiveCodeSystems) {}
}
