package org.opencds.cqf.fhir.cr.implementationguide;

import java.util.HashSet;
import java.util.Set;

/**
 * Result of key element analysis and filtering.
 * Contains the ValueSets bound to key elements, the CodeSystems they reference,
 * and any ValueSets referenced by key element ValueSets.
 */
public class KeyElementFilteringResult {
    private final Set<String> keyElementValueSets;
    private final Set<String> referencedCodeSystems;
    private final Set<String> referencedValueSets;
    private final boolean filteringEnabled;

    public KeyElementFilteringResult(
            Set<String> keyElementValueSets,
            Set<String> referencedCodeSystems,
            Set<String> referencedValueSets,
            boolean filteringEnabled) {
        this.keyElementValueSets = new HashSet<>(keyElementValueSets);
        this.referencedCodeSystems = new HashSet<>(referencedCodeSystems);
        this.referencedValueSets = new HashSet<>(referencedValueSets);
        this.filteringEnabled = filteringEnabled;
    }

    /**
     * Returns ValueSet canonical URLs that are bound to key elements in StructureDefinitions.
     */
    public Set<String> getKeyElementValueSets() {
        return keyElementValueSets;
    }

    /**
     * Returns CodeSystem URLs referenced by key element ValueSets.
     */
    public Set<String> getReferencedCodeSystems() {
        return referencedCodeSystems;
    }

    /**
     * Returns ValueSet URLs referenced by key element ValueSets (via compose.include.valueSet).
     */
    public Set<String> getReferencedValueSets() {
        return referencedValueSets;
    }

    /**
     * Returns true if key element filtering should be applied.
     * False if no StructureDefinitions were found (fallback to include all).
     */
    public boolean isFilteringEnabled() {
        return filteringEnabled;
    }

    /**
     * Determines if a dependency should be included based on key element analysis.
     *
     * @param canonical the canonical URL of the dependency
     * @param resourceType the FHIR resource type
     * @return true if the dependency should be included
     */
    public boolean shouldIncludeDependency(String canonical, String resourceType) {
        if (canonical == null || canonical.isEmpty()) {
            return false;
        }

        // Only include ValueSet and CodeSystem resources
        if (resourceType == null || (!resourceType.equals("ValueSet") && !resourceType.equals("CodeSystem"))) {
            return false;
        }

        // Fallback mode: if no key element ValueSets were found, include all terminology
        if (!filteringEnabled) {
            return true;
        }

        String canonicalNoVersion = canonical.split("\\|")[0];

        // For ValueSets, check if it's a key element ValueSet OR referenced by a key element ValueSet
        if (resourceType.equals("ValueSet")) {
            boolean isKeyElement = keyElementValueSets.stream()
                    .anyMatch(vs -> vs.equals(canonical) || vs.startsWith(canonicalNoVersion));
            boolean isReferenced = referencedValueSets.contains(canonical)
                    || referencedValueSets.contains(canonicalNoVersion);
            return isKeyElement || isReferenced;
        }

        // For CodeSystems, check if referenced by key element ValueSets
        if (resourceType.equals("CodeSystem")) {
            return referencedCodeSystems.contains(canonicalNoVersion);
        }

        return false;
    }
}
