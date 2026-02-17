package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.UsageContext;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class KnowledgeArtifactProcessor {

    private KnowledgeArtifactProcessor() {}

    public static final String releaseLabelUrl = "http://hl7.org/fhir/StructureDefinition/artifact-releaseLabel";
    public static final String releaseDescriptionUrl =
            "http://hl7.org/fhir/StructureDefinition/artifact-releaseDescription";
    public static final String valueSetPriorityCode = "priority";
    public static final String valueSetConditionCode = "focus";
    public static final List<String> preservedExtensionUrls =
            List.of(TransformProperties.vsmPriority, TransformProperties.vsmCondition);

    /**
     * ValueSets can be part of multiple artifacts at the same time. Certain properties are tracked/managed in the manifest to avoid conflicts with other artifacts. This function sets those properties on the ValueSets themselves at export / $package time
     * @param manifest the resource containing all RelatedArtifact references
     * @param bundleEntries the list of packaged resources to modify according to the extensions on the manifest relatedArtifact references
     */
    public static void handleValueSetReferenceExtensions(
            MetadataResource manifest, List<BundleEntryComponent> bundleEntries)
            throws UnprocessableEntityException, IllegalArgumentException {
        var adapter = IAdapterFactory.forFhirVersion(FhirVersionEnum.R4).createKnowledgeArtifactAdapter(manifest);
        var urlValueSetMap = populateUrlValueSetMap(bundleEntries);
        var relatedArtifactsWithPreservedExtension =
                getRelatedArtifactsWithPreservedExtensions(adapter.getDependencies());
        var relatedArtifactMap = new HashMap<String, IDependencyInfo>();
        relatedArtifactsWithPreservedExtension.forEach(ra -> {
            relatedArtifactMap.put(Canonicals.getUrl(ra.getReference()), ra);
        });
        Map<String, Set<String>> ancestorsMap = null;
        for (final var valueSet : urlValueSetMap.values()) {
            // remove any existing Priority and Conditions
            var usageContexts = removeExistingReferenceExtensionData(valueSet.getUseContext());
            valueSet.setUseContext(usageContexts);
            Optional<IDependencyInfo> maybeVSRelatedArtifact = Optional.empty();
            if (!isGrouper(valueSet)) {
                boolean valueSetNeedsCondition = true;
                // for leaf valuesets we want to be as forgiving as possible
                // since the reference might be to a transitive dependency
                // First check direct references
                if (relatedArtifactMap.containsKey(valueSet.getUrl())
                        && !checkIfValueSetNeedsCondition(valueSet, relatedArtifactMap.get(valueSet.getUrl()), null)) {
                    valueSetNeedsCondition = false;
                    maybeVSRelatedArtifact = Optional.of(relatedArtifactMap.get(valueSet.getUrl()));
                }
                // Then check transitive references
                if (valueSetNeedsCondition) {
                    // generate only if needed because it's an expensive operation
                    if (ancestorsMap == null) {
                        ancestorsMap = generateAncestorsMap(urlValueSetMap);
                    }
                    final var ancestors = ancestorsMap.get(valueSet.getUrl());
                    if (ancestors != null) {
                        for (final var ancestor : ancestors) {
                            if (!isGrouper(urlValueSetMap.get(ancestor))
                                    // take the first ancestor relatedArtifact which HAS a valid VSM Condition
                                    // and is NOT a grouper
                                    && !checkIfValueSetNeedsCondition(
                                            valueSet, relatedArtifactMap.get(ancestor), null)) {
                                valueSetNeedsCondition = false;
                                maybeVSRelatedArtifact = Optional.of(relatedArtifactMap.get(ancestor));
                                break;
                            }
                        }
                    }
                }
                if (valueSetNeedsCondition) {
                    throw new UnprocessableEntityException("Missing condition on leaf ValueSet : " + valueSet.getUrl());
                }
                maybeVSRelatedArtifact
                        .map(IDependencyInfo::getExtension)
                        .ifPresent(
                                // add Conditions
                                exts -> {
                                    exts.stream()
                                            .filter(ext ->
                                                    ext.getUrl().equalsIgnoreCase(TransformProperties.vsmCondition))
                                            .forEach(ext ->
                                                    tryAddCondition(usageContexts, (CodeableConcept) ext.getValue()));
                                });
            } else {
                maybeVSRelatedArtifact = Optional.ofNullable(relatedArtifactMap.get(valueSet.getUrl()));
            }
            // update Priority
            var priority = getOrCreateUsageContext(
                    usageContexts, TransformProperties.usPHUsageContextType, valueSetPriorityCode);
            maybeVSRelatedArtifact
                    .flatMap(ra -> ra.getExtension().stream()
                            .map(e -> (Extension) e)
                            .filter(e -> e.getUrl().equals(TransformProperties.vsmPriority))
                            .findAny())
                    .ifPresentOrElse(
                            // set value as per extension
                            ext -> priority.setValue(ext.getValue()),
                            // set to "routine" if missing
                            () -> {
                                var routine = new CodeableConcept(
                                                new Coding(TransformProperties.usPHUsageContext, "routine", null))
                                        .setText("Routine");
                                priority.setValue(routine);
                            });
        }
    }

    private static Map<String, ValueSet> populateUrlValueSetMap(List<BundleEntryComponent> bundleEntries) {
        Map<String, ValueSet> urlValueSetMap = new HashMap<>();
        bundleEntries.stream()
                .filter(e -> e.getResource().getResourceType().equals(ResourceType.ValueSet))
                .map(e -> (ValueSet) e.getResource())
                .forEach(vs -> urlValueSetMap.put(vs.getUrl(), vs));
        return urlValueSetMap;
    }

    public static List<IDependencyInfo> getRelatedArtifactsWithPreservedExtensions(List<IDependencyInfo> deps) {
        return deps.stream()
                .filter(ra -> preservedExtensionUrls.stream().anyMatch(url -> ra.getExtension().stream()
                        .anyMatch(ext -> ext.getUrl().equalsIgnoreCase(url))))
                .collect(Collectors.toList());
    }

    /**
     * Removes any existing UsageContexts corresponding to the VSM-specific extensions
     * @param usageContexts the list of usage contexts to modify
     */
    public static List<UsageContext> removeExistingReferenceExtensionData(List<UsageContext> usageContexts) {
        var useContextCodesToReplace = List.of(valueSetConditionCode, valueSetPriorityCode);
        return usageContexts.stream()
                // remove any useContexts which need to be replaced
                .filter(useContext -> useContextCodesToReplace.stream()
                        .noneMatch(code -> useContext.getCode().getCode().equals(code)))
                .collect(Collectors.toList());
    }

    /**
     * Determines whether a given ValueSet is a grouper
     * @param resource
     * @return
     */
    public static boolean isGrouper(MetadataResource resource) {
        return resource.getResourceType() == ResourceType.ValueSet
                && resource.getUseContext().stream()
                        .anyMatch(uc -> uc.hasCode() && uc.getCode().getCode().equals(TransformProperties.grouperType));
    }

    public static boolean checkIfValueSetNeedsCondition(
            MetadataResource resource, IDependencyInfo relatedArtifact, IRepository hapiFhirRepository)
            throws UnprocessableEntityException {
        if (resource == null
                && relatedArtifact != null
                && relatedArtifact.getReference() != null
                && Canonicals.getResourceType(relatedArtifact.getReference()).equals("ValueSet")) {
            var searchResults = BundleHelper.getEntryResources(SearchHelper.searchRepositoryByCanonicalWithPaging(
                    hapiFhirRepository, relatedArtifact.getReference()));
            if (!searchResults.isEmpty()) {
                resource = (MetadataResource) searchResults.get(0);
            }
        }
        if (resource != null && resource.getResourceType() == ResourceType.ValueSet) {
            var valueSet = (ValueSet) resource;
            var isLeaf = !isGrouper(valueSet);
            var maybeConditionExtension = Optional.ofNullable(relatedArtifact)
                    .map(IDependencyInfo::getExtension)
                    .flatMap(list -> list.stream()
                            .map(e -> (Extension) e)
                            .filter(ext -> ext.getUrl().equalsIgnoreCase(TransformProperties.vsmCondition))
                            .findFirst());
            return isLeaf && maybeConditionExtension.isEmpty();
        }
        return false;
    }

    private static Map<String, Set<String>> generateAncestorsMap(Map<String, ValueSet> valueSetMap) {
        Map<String, Set<String>> ancestorsMap = new HashMap<>();
        for (final var valueSet : valueSetMap.values()) {
            populateAncestry(valueSet, valueSetMap, ancestorsMap);
        }
        return ancestorsMap;
    }

    private static void populateAncestry(
            ValueSet vs, Map<String, ValueSet> valueSetMap, Map<String, Set<String>> ancestorsMap) {
        final var currentAncestor = vs.getUrl();
        for (final var include : vs.getCompose().getInclude()) {
            for (final var canonical : include.getValueSet()) {
                final var leafUrl = Canonicals.getUrl(canonical.getValue());
                if (!ancestorsMap.containsKey(leafUrl)) {
                    ancestorsMap.put(leafUrl, new HashSet<>());
                }
                final var ancestors = ancestorsMap.get(leafUrl);
                ancestors.add(currentAncestor);
                if (valueSetMap.containsKey(leafUrl)) {
                    populateAncestry(valueSetMap.get(leafUrl), valueSetMap, ancestorsMap);
                }
            }
        }
    }

    public static void tryAddCondition(List<UsageContext> usageContexts, CodeableConcept condition) {
        var focusAlreadyExists = usageContexts.stream()
                .anyMatch(u -> u.getCode().getSystem().equals(TransformProperties.hl7UsageContextType)
                        && u.getCode().getCode().equals(valueSetConditionCode)
                        && u.getValueCodeableConcept()
                                .hasCoding(
                                        condition.getCoding().get(0).getSystem(),
                                        condition.getCoding().get(0).getCode()));
        if (!focusAlreadyExists) {
            var newFocus = new UsageContext(
                    new Coding(TransformProperties.hl7UsageContextType, valueSetConditionCode, null), condition);
            newFocus.setValue(condition);
            usageContexts.add(newFocus);
        }
    }

    /**
     *
     * Either finds a usageContext with the same system and code or creates an empty one
     * and appends it
     *
     * @param usageContexts the list of usageContexts to search and/or append to
     * @param system the usageContext.code.system to find / create
     * @param code the usage.code.code to find / create
     * @return the found / created usageContext
     */
    public static UsageContext getOrCreateUsageContext(List<UsageContext> usageContexts, String system, String code) {
        return usageContexts.stream()
                .filter(useContext -> useContext.getCode().getSystem().equals(system)
                        && useContext.getCode().getCode().equals(code))
                .findFirst()
                .orElseGet(() -> {
                    // create the UseContext if it doesn't exist
                    var c = new Coding(system, code, null);
                    var n = new UsageContext(c, null);
                    // add it to the ValueSet before returning
                    usageContexts.add(n);
                    return n;
                });
    }
}
