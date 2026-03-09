package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes StructureDefinitions to determine key elements and their associated ValueSet bindings.
 * <p>
 * Aligned with the IG Publisher's {@code StructureDefinitionRenderer.scanForKeyElements()} procedure.
 * <p>
 * Key element criteria (from IG Publisher lines 680-762):
 * <ol>
 *   <li>mustSupport (with ancestors)</li>
 *   <li>min != 0 (mandatory)</li>
 *   <li>hasCondition (invariant references, count > 1)</li>
 *   <li>isModifier</li>
 *   <li>hasSlicing (non-extension paths)</li>
 *   <li>hasSliceName</li>
 *   <li>in differential</li>
 *   <li>max constrained from base</li>
 *   <li>min constrained from base</li>
 *   <li>binding changed from base</li>
 *   <li>hasFixed</li>
 *   <li>hasPattern</li>
 *   <li>hasMaxLength</li>
 *   <li>R5 key constraints (mustHaveValue, valueAlternatives, minValue, maxValue)</li>
 *   <li>significant extensions</li>
 * </ol>
 */
public class KeyElementAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(KeyElementAnalyzer.class);

    private static final List<String> SIGNIFICANT_EXTENSIONS = List.of(
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-allowedUnits",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-bestPractice",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-graphConstraint",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxDecimalPlaces",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-maxSize",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-mimeType",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-minLength",
            "http://hl7.org/fhir/StructureDefinition/elementdefinition-obligation");

    private final ConformanceResourceResolver resolver;
    private final IAdapterFactory adapterFactory;

    public KeyElementAnalyzer(ConformanceResourceResolver resolver, FhirVersionEnum fhirVersion) {
        this.resolver = resolver;
        this.adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
    }

    /**
     * Analyzes a StructureDefinition to extract ValueSet canonical URLs that are bound to key elements.
     * Walks the inheritance chain applying the key element procedure at each level.
     *
     * @param structureDefinition the StructureDefinition resource to analyze
     * @return set of ValueSet canonical URLs bound to key elements
     */
    public Set<String> getKeyElementValueSets(IBaseResource structureDefinition) {
        Set<String> valueSets = new HashSet<>();

        if (structureDefinition == null) {
            return valueSets;
        }

        // Step D: Walk the inheritance chain
        IBaseResource currentProfile = structureDefinition;
        Set<String> visited = new HashSet<>();

        while (currentProfile != null) {
            IStructureDefinitionAdapter sd = adapterFactory.createStructureDefinition(currentProfile);
            String url = sd.getUrl();

            // Prevent infinite loops
            if (url != null && !visited.add(url)) {
                break;
            }

            // Steps A-C for current profile
            valueSets.addAll(analyzeProfile(sd));

            // Move to base definition
            currentProfile = resolveBaseDefinition(sd);
        }

        return valueSets;
    }

    /**
     * Analyzes a single profile level: key eligibility, mustSupport scan,
     * differential hash, scanForKeyElements, and binding extraction.
     */
    private Set<String> analyzeProfile(IStructureDefinitionAdapter sd) {
        Set<String> valueSets = new HashSet<>();

        try {
            // Step 1: Key eligibility guard
            String derivation = sd.getDerivation();
            if (!"constraint".equalsIgnoreCase(derivation)) {
                return valueSets;
            }

            // Get snapshot elements (required for proper analysis)
            List<IElementDefinitionAdapter> snapshotElements = sd.getAllSnapshotElements();
            if (snapshotElements.isEmpty()) {
                // Fall back to differential if no snapshot
                snapshotElements = sd.getAllDifferentialElements();
                if (snapshotElements.isEmpty()) {
                    return valueSets;
                }
            }

            // Step 2: Build mustSupport map
            Map<String, Boolean> mustSupportMap = buildMustSupportMap(snapshotElements);

            // Step 3: Build differential hash
            Set<String> differentialIds = buildDifferentialHash(sd);

            // Step 4: scanForKeyElements
            Set<String> keyElementIds = scanForKeyElements(snapshotElements, mustSupportMap, differentialIds, sd);

            // Step 5: Extract bindings from key elements
            for (var element : snapshotElements) {
                String id = element.getId();
                if (id != null && keyElementIds.contains(id)) {
                    String valueSetUrl = element.getBindingValueSet();
                    if (valueSetUrl != null && !valueSetUrl.isEmpty()) {
                        valueSets.add(valueSetUrl);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error analyzing profile for key elements", e);
        }

        return valueSets;
    }

    /**
     * Builds the mustSupport map by walking snapshot elements as a tree.
     * When element has mustSupport == true, add it AND all ancestors to map.
     * (IG Publisher lines 644-658)
     */
    private Map<String, Boolean> buildMustSupportMap(List<IElementDefinitionAdapter> elements) {
        Map<String, Boolean> mustSupportMap = new HashMap<>();

        for (var element : elements) {
            if (element.getMustSupport()) {
                String id = element.getId();
                if (id != null) {
                    mustSupportMap.put(id, true);
                    // Add all ancestors
                    for (String ancestorId : getAncestorIds(id, elements)) {
                        mustSupportMap.putIfAbsent(ancestorId, false);
                    }
                }
            }
        }

        return mustSupportMap;
    }

    /**
     * Builds the set of element IDs present in the differential.
     * (IG Publisher lines 609-627)
     */
    private Set<String> buildDifferentialHash(IStructureDefinitionAdapter sd) {
        Set<String> differentialIds = new HashSet<>();

        List<IElementDefinitionAdapter> diffElements = sd.getAllDifferentialElements();
        for (var element : diffElements) {
            String id = element.getId();
            if (id != null) {
                differentialIds.add(id);
            }
        }

        return differentialIds;
    }

    /**
     * Scans elements for key element criteria, aligned with IG Publisher scanForKeyElements().
     * (IG Publisher lines 680-762)
     */
    private Set<String> scanForKeyElements(
            List<IElementDefinitionAdapter> elements,
            Map<String, Boolean> mustSupportMap,
            Set<String> differentialIds,
            IStructureDefinitionAdapter sd) {

        Set<String> keyElements = new HashSet<>();

        if (elements.isEmpty()) {
            return keyElements;
        }

        // Always add root element
        var rootElement = elements.get(0);
        String rootId = rootElement.getId();
        if (rootId != null) {
            keyElements.add(rootId);
        }

        // Scan direct children of root, then recurse
        scanChildren(elements, rootElement, keyElements, mustSupportMap, differentialIds, sd);

        return keyElements;
    }

    /**
     * Recursively scans children of a parent element for key criteria.
     */
    private void scanChildren(
            List<IElementDefinitionAdapter> allElements,
            IElementDefinitionAdapter parent,
            Set<String> keyElements,
            Map<String, Boolean> mustSupportMap,
            Set<String> differentialIds,
            IStructureDefinitionAdapter sd) {

        List<IElementDefinitionAdapter> children = getDirectChildren(allElements, parent);

        for (var child : children) {
            String childId = child.getId();
            if (childId == null) {
                continue;
            }

            if (isKeyElement(child, mustSupportMap, differentialIds)) {
                keyElements.add(childId);
                // Recursively scan this child's children
                scanChildren(allElements, child, keyElements, mustSupportMap, differentialIds, sd);
            }
        }
    }

    /**
     * Evaluates all key element criteria for a single element.
     */
    private boolean isKeyElement(
            IElementDefinitionAdapter element, Map<String, Boolean> mustSupportMap, Set<String> differentialIds) {
        return isKeyByStructuralCriteria(element, mustSupportMap, differentialIds)
                || isKeyByConstraintCriteria(element);
    }

    private boolean isKeyByStructuralCriteria(
            IElementDefinitionAdapter element, Map<String, Boolean> mustSupportMap, Set<String> differentialIds) {
        String id = element.getId();
        String path = element.getPath();

        // mustSupport
        if (mustSupportMap.containsKey(id)) {
            return true;
        }
        // min != 0 (mandatory)
        if (element.hasMin() && element.getMin() != 0) {
            return true;
        }
        // hasCondition (invariant references)
        if (element.hasCondition()) {
            return true;
        }
        // isModifier
        if (element.isModifier()) {
            return true;
        }
        // hasSlicing on non-extension paths
        if (element.hasSlicing()
                && path != null
                && !path.endsWith(".extension")
                && !path.endsWith(".modifierExtension")) {
            return true;
        }
        // hasSliceName
        String sliceName = element.getSliceName();
        if (sliceName != null && !sliceName.isEmpty()) {
            return true;
        }
        // in differential
        return differentialIds.contains(id);
    }

    private boolean isKeyByConstraintCriteria(IElementDefinitionAdapter element) {
        // max constrained from base
        if (element.hasMax()) {
            String max = element.getMax();
            String baseMax = element.getBaseMax();
            if (max != null && baseMax != null && !max.equals(baseMax)) {
                return true;
            }
        }
        // min constrained from base
        if (element.hasMin() && element.getMin() != element.getBaseMin()) {
            return true;
        }
        // binding changed from base
        if (isBindingChangedFromBase(element)) {
            return true;
        }
        return element.hasFixed()
                || element.hasPattern()
                || element.hasMaxLength()
                || element.hasR5KeyConstraints()
                || hasSignificantExtensions(element);
    }

    /**
     * Checks if the element's binding has changed from its base definition.
     * (IG Publisher lines 720-740)
     */
    private boolean isBindingChangedFromBase(IElementDefinitionAdapter element) {
        if (!element.hasBinding()) {
            return false;
        }

        String childStrength = element.getBindingStrength();
        String childValueSet = element.getBindingValueSet();

        // Only check for required/extensible bindings
        if (childStrength == null || (!"required".equals(childStrength) && !"extensible".equals(childStrength))) {
            return false;
        }

        // Find the base element to compare
        String basePath = element.getBasePath();
        if (basePath == null || !basePath.contains(".")) {
            return false;
        }

        String baseTypeName = basePath.substring(0, basePath.indexOf("."));
        String baseCanonical = "http://hl7.org/fhir/StructureDefinition/" + baseTypeName;

        IBaseResource baseSd = resolver != null ? resolver.resolveStructureDefinition(baseCanonical) : null;
        if (baseSd == null) {
            // Conservative: if we can't resolve base, treat as changed
            return true;
        }

        IStructureDefinitionAdapter baseSdAdapter = adapterFactory.createStructureDefinition(baseSd);
        List<IElementDefinitionAdapter> baseElements = baseSdAdapter.getAllSnapshotElements();

        // Find matching element by path
        IElementDefinitionAdapter baseElement = null;
        for (var be : baseElements) {
            if (basePath.equals(be.getPath())) {
                baseElement = be;
                break;
            }
        }

        if (baseElement == null) {
            // Conservative: base element not found
            return true;
        }

        // Compare bindings
        if (!baseElement.hasBinding()) {
            // Child has binding but base doesn't
            return true;
        }

        String baseStrength = baseElement.getBindingStrength();
        String baseValueSet = baseElement.getBindingValueSet();

        // Check if strength or valueSet differs
        return !safeEquals(childStrength, baseStrength) || !safeEquals(childValueSet, baseValueSet);
    }

    /**
     * Checks if the element has any significant extensions.
     */
    private boolean hasSignificantExtensions(IElementDefinitionAdapter element) {
        List<String> extensionUrls = element.getExtensionUrls();
        if (extensionUrls == null || extensionUrls.isEmpty()) {
            return false;
        }

        for (String url : extensionUrls) {
            if (SIGNIFICANT_EXTENSIONS.contains(url)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets direct children of a parent element from the element list.
     * (IG Publisher lines 1772-1788)
     */
    private List<IElementDefinitionAdapter> getDirectChildren(
            List<IElementDefinitionAdapter> elements, IElementDefinitionAdapter parent) {

        String parentPath = parent.getPath();
        if (parentPath == null) {
            return List.of();
        }

        int parentDepth = parentPath.split("\\.").length;

        return elements.stream()
                .filter(e -> {
                    String ePath = e.getPath();
                    if (ePath == null || !ePath.startsWith(parentPath + ".")) {
                        return false;
                    }
                    // Direct child = depth exactly one more than parent
                    // But also include slices (same path, different sliceName)
                    int eDepth = ePath.split("\\.").length;
                    return eDepth == parentDepth + 1;
                })
                .toList();
    }

    /**
     * Gets ancestor IDs for a given element ID from the element list.
     */
    private Set<String> getAncestorIds(String elementId, List<IElementDefinitionAdapter> elements) {
        if (elementId == null || !elementId.contains(".")) {
            return new HashSet<>();
        }

        var elementPath = findPathById(elementId, elements);
        if (elementPath == null || !elementPath.contains(".")) {
            return new HashSet<>();
        }

        var pathToId = buildPathToIdMap(elements);
        return collectAncestorIds(elementPath, pathToId);
    }

    private String findPathById(String elementId, List<IElementDefinitionAdapter> elements) {
        for (var e : elements) {
            if (elementId.equals(e.getId())) {
                return e.getPath();
            }
        }
        return null;
    }

    private Map<String, String> buildPathToIdMap(List<IElementDefinitionAdapter> elements) {
        Map<String, String> pathToId = new HashMap<>();
        for (var e : elements) {
            if (e.getPath() != null && e.getId() != null) {
                pathToId.put(e.getPath(), e.getId());
            }
        }
        return pathToId;
    }

    private Set<String> collectAncestorIds(String elementPath, Map<String, String> pathToId) {
        Set<String> ancestors = new HashSet<>();
        String[] parts = elementPath.split("\\.");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                current.append(".");
            }
            current.append(parts[i]);
            String ancestorId = pathToId.get(current.toString());
            if (ancestorId != null) {
                ancestors.add(ancestorId);
            }
        }
        return ancestors;
    }

    /**
     * Resolves the base definition for inheritance walking.
     */
    private IBaseResource resolveBaseDefinition(IStructureDefinitionAdapter sd) {
        try {
            var baseDefElement = sd.getBaseDefinition();
            if (baseDefElement == null) {
                return null;
            }

            String baseDefUrl = baseDefElement.getValueAsString();
            if (baseDefUrl == null || baseDefUrl.isEmpty()) {
                return null;
            }

            if (resolver != null) {
                return resolver.resolveStructureDefinition(baseDefUrl);
            }
        } catch (Exception e) {
            logger.debug("Error resolving base definition", e);
        }

        return null;
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
