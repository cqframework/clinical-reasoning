package org.opencds.cqf.fhir.cr.implementationguide;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.visitor.KeyElementAnalyzer;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;

/**
 * Filters terminology resources (ValueSets and CodeSystems) based on key element analysis.
 * Only includes terminology that is bound to key elements (mustSupport, differential elements,
 * mandatory children, slices, modifiers, etc.) in StructureDefinitions.
 */
public class KeyElementFilter {
    private final KeyElementAnalyzer analyzer;

    public KeyElementFilter(IRepository repository) {
        this.analyzer = new KeyElementAnalyzer(repository);
    }

    /**
     * Performs key element analysis on StructureDefinitions from the main IG.
     *
     * @param gatheredResources All resources gathered from packages
     * @param resourceSourcePackages Mapping of resource canonicals to source package canonicals
     * @param mainIgCanonical The canonical URL of the main ImplementationGuide
     * @return KeyElementFilteringResult containing filtering criteria
     */
    public KeyElementFilteringResult analyzeKeyElements(
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            Map<String, String> resourceSourcePackages,
            String mainIgCanonical) {

        IAdapter.logger.info(
                "Phase 2: Analyzing key elements from MAIN IG StructureDefinitions (excluding dependency IGs)");

        var keyElementValueSets =
                identifyKeyElementValueSets(gatheredResources, resourceSourcePackages, mainIgCanonical);

        boolean filteringEnabled = !keyElementValueSets.isEmpty();

        if (!filteringEnabled) {
            IAdapter.logger.warn(
                    "No key element ValueSets found in gathered StructureDefinitions. "
                            + "Key element filtering will be disabled - all ValueSets and CodeSystems from dependency IGs will be included. "
                            + "This typically means the IG's StructureDefinitions are not available in the repository or package registry.");
        } else {
            long totalStructureDefinitions = gatheredResources.values().stream()
                    .filter(r -> r.get().fhirType().equals("StructureDefinition"))
                    .count();
            long mainIgStructureDefinitions = gatheredResources.values().stream()
                    .filter(r -> r.get().fhirType().equals("StructureDefinition"))
                    .filter(r -> {
                        String canonical = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                        String sourcePackage =
                                resourceSourcePackages.getOrDefault(canonical, resourceSourcePackages.get(r.getUrl()));
                        return mainIgCanonical.equals(sourcePackage);
                    })
                    .count();
            IAdapter.logger.info(
                    "Key element filtering enabled. Found {} key element ValueSets from {} main IG StructureDefinitions (out of {} total StructureDefinitions).",
                    keyElementValueSets.size(),
                    mainIgStructureDefinitions,
                    totalStructureDefinitions);
        }

        // Pre-compute referenced CodeSystems and ValueSets for performance
        var referencedCodeSystems = preComputeReferencedCodeSystems(keyElementValueSets, gatheredResources);
        var referencedValueSets = preComputeReferencedValueSets(keyElementValueSets, gatheredResources);

        return new KeyElementFilteringResult(
                keyElementValueSets, referencedCodeSystems, referencedValueSets, filteringEnabled);
    }

    /**
     * Identifies ValueSets that are bound to key elements in the gathered StructureDefinitions.
     * Only analyzes StructureDefinitions from the MAIN IG, not dependency IGs.
     */
    private Set<String> identifyKeyElementValueSets(
            Map<String, IKnowledgeArtifactAdapter> gatheredResources,
            Map<String, String> resourceSourcePackages,
            String mainIgCanonical) {

        var keyElementValueSets = new HashSet<String>();

        for (var resourceAdapter : gatheredResources.values()) {
            var resource = resourceAdapter.get();
            // Only analyze StructureDefinitions
            if (resource.fhirType().equals("StructureDefinition")) {
                // CRITICAL: Only analyze StructureDefinitions from the MAIN IG, not dependency IGs
                // This prevents bloat from analyzing hundreds of base FHIR StructureDefinitions
                String canonical = resourceAdapter.hasVersion()
                        ? resourceAdapter.getUrl() + "|" + resourceAdapter.getVersion()
                        : resourceAdapter.getUrl();
                String sourcePackage = resourceSourcePackages.getOrDefault(
                        canonical, resourceSourcePackages.get(resourceAdapter.getUrl()));

                if (!mainIgCanonical.equals(sourcePackage)) {
                    IAdapter.logger.debug(
                            "Skipping StructureDefinition from dependency package {}: {}",
                            sourcePackage,
                            resourceAdapter.getUrl());
                    continue;
                }

                var valueSets = analyzer.getKeyElementValueSets(resource);
                keyElementValueSets.addAll(valueSets);
                IAdapter.logger.debug(
                        "Found {} key element ValueSets in MAIN IG StructureDefinition: {}",
                        valueSets.size(),
                        resourceAdapter.getUrl());
            }
        }

        IAdapter.logger.info("Total key element ValueSets identified from main IG: {}", keyElementValueSets.size());
        return keyElementValueSets;
    }

    /**
     * Pre-computes CodeSystems referenced by key element ValueSets.
     * This optimization builds a lookup set of CodeSystem URLs to avoid expensive
     * repeated ValueSet parsing during dependency filtering.
     */
    private Set<String> preComputeReferencedCodeSystems(
            Set<String> keyElementValueSets, Map<String, IKnowledgeArtifactAdapter> gatheredResources) {

        var referencedCodeSystems = new HashSet<String>();

        if (!keyElementValueSets.isEmpty()) {
            IAdapter.logger.info("Pre-computing CodeSystems referenced by key element ValueSets...");
            for (String vsCanonical : keyElementValueSets) {
                String vsCanonicalNoVersion = vsCanonical.split("\\|")[0];
                var valueSetAdapter = gatheredResources.values().stream()
                        .filter(r -> r.get().fhirType().equals("ValueSet"))
                        .filter(r -> {
                            String url = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                            return url.equals(vsCanonical) || r.getUrl().equals(vsCanonicalNoVersion);
                        })
                        .findFirst();

                if (valueSetAdapter.isPresent()) {
                    var codeSystems =
                            extractCodeSystemsFromValueSet(valueSetAdapter.get().get());
                    referencedCodeSystems.addAll(codeSystems);
                }
            }
            IAdapter.logger.info(
                    "Found {} CodeSystems referenced by {} key element ValueSets",
                    referencedCodeSystems.size(),
                    keyElementValueSets.size());
        }
        return referencedCodeSystems;
    }

    /**
     * Pre-computes ValueSets referenced by key element ValueSets.
     * This optimization builds a lookup set of ValueSet URLs to avoid expensive
     * repeated ValueSet parsing during dependency filtering.
     */
    private Set<String> preComputeReferencedValueSets(
            Set<String> keyElementValueSets, Map<String, IKnowledgeArtifactAdapter> gatheredResources) {

        var referencedValueSets = new HashSet<String>();

        if (!keyElementValueSets.isEmpty()) {
            IAdapter.logger.info("Pre-computing ValueSets referenced by key element ValueSets...");
            for (String vsCanonical : keyElementValueSets) {
                String vsCanonicalNoVersion = vsCanonical.split("\\|")[0];
                var valueSetAdapter = gatheredResources.values().stream()
                        .filter(r -> r.get().fhirType().equals("ValueSet"))
                        .filter(r -> {
                            String url = r.hasVersion() ? r.getUrl() + "|" + r.getVersion() : r.getUrl();
                            return url.equals(vsCanonical) || r.getUrl().equals(vsCanonicalNoVersion);
                        })
                        .findFirst();

                if (valueSetAdapter.isPresent()) {
                    var valueSets = extractValueSetsFromValueSet(valueSetAdapter.get().get());
                    referencedValueSets.addAll(valueSets);
                }
            }
            IAdapter.logger.info(
                    "Found {} ValueSets referenced by {} key element ValueSets",
                    referencedValueSets.size(),
                    keyElementValueSets.size());
        }
        return referencedValueSets;
    }

    /**
     * Extracts all CodeSystem URLs referenced by a ValueSet resource.
     * Only includes CodeSystems from compose.include, NOT compose.exclude.
     */
    private Set<String> extractCodeSystemsFromValueSet(IBaseResource valueSet) {
        var codeSystems = new HashSet<String>();
        try {
            FhirVersionEnum fhirVersion = valueSet.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();

            // Only check compose.include[].system - these are the CodeSystems being used
            // Do NOT include compose.exclude[].system - those are exclusions, not dependencies
            var includeSystems = fhirPath.evaluate(valueSet, "ValueSet.compose.include.system", IPrimitiveType.class);
            for (var system : includeSystems) {
                String systemUrl = system.getValueAsString();
                if (systemUrl != null && !systemUrl.isEmpty()) {
                    codeSystems.add(systemUrl);
                }
            }
        } catch (Exception e) {
            IAdapter.logger.debug("Error extracting CodeSystems from ValueSet", e);
        }

        return codeSystems;
    }

    /**
     * Extracts all ValueSet URLs referenced by a ValueSet resource.
     * Only includes ValueSets from compose.include, NOT compose.exclude.
     */
    private Set<String> extractValueSetsFromValueSet(IBaseResource valueSet) {
        var valueSets = new HashSet<String>();
        try {
            FhirVersionEnum fhirVersion = valueSet.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();

            // Extract compose.include[].valueSet - these are ValueSets being included
            // Do NOT include compose.exclude[].valueSet - those are exclusions, not dependencies
            var includeValueSets =
                    fhirPath.evaluate(valueSet, "ValueSet.compose.include.valueSet", IPrimitiveType.class);
            for (var vs : includeValueSets) {
                String vsUrl = vs.getValueAsString();
                if (vsUrl != null && !vsUrl.isEmpty()) {
                    valueSets.add(vsUrl);
                }
            }
        } catch (Exception e) {
            IAdapter.logger.debug("Error extracting ValueSets from ValueSet", e);
        }

        return valueSets;
    }
}
