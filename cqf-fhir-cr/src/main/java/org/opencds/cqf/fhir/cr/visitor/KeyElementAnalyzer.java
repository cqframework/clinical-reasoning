package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.utility.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes StructureDefinitions to determine key elements and their associated ValueSet bindings.
 * <p>
 * Implements the key element procedure:
 * <ul>
 *   <li>Step A: Build seed set (mustSupport, differential, ancestors)</li>
 *   <li>Step B: Expand downward (mandatory children, constrained, slices, modifiers)</li>
 *   <li>Step C: Extract bindings from key elements</li>
 *   <li>Step D: Walk inheritance chain applying A-C at each level</li>
 * </ul>
 */
public class KeyElementAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(KeyElementAnalyzer.class);
    private final IRepository repository;

    public KeyElementAnalyzer(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Analyzes a StructureDefinition to extract ValueSet canonical URLs that are bound to key elements.
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
        while (currentProfile != null) {
            // Steps A-C for current profile
            Set<String> keyElements = getKeyElements(currentProfile);
            valueSets.addAll(extractBindingsFromKeyElements(currentProfile, keyElements));

            // Move to base definition
            currentProfile = getBaseDefinition(currentProfile);
        }

        return valueSets;
    }

    /**
     * Step A: Build seed set of key elements.
     * Step B: Expand downward recursively.
     *
     * @param structureDefinition the StructureDefinition to analyze
     * @return set of element paths that are key elements
     */
    private Set<String> getKeyElements(IBaseResource structureDefinition) {
        Set<String> keyElements = new HashSet<>();

        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();

            // Step A: Build seed set
            Set<String> seedSet = buildSeedSet(structureDefinition);
            keyElements.addAll(seedSet);

            // Step B: Expand downward for each seed element
            Set<String> elementsToProcess = new HashSet<>(seedSet);
            Set<String> processed = new HashSet<>();

            while (!elementsToProcess.isEmpty()) {
                Set<String> newElements = new HashSet<>();

                for (String elementPath : elementsToProcess) {
                    if (processed.contains(elementPath)) {
                        continue;
                    }
                    processed.add(elementPath);

                    // Find children that meet expansion criteria
                    List<IBase> children = getChildElementsForExpansion(structureDefinition, elementPath);
                    for (IBase child : children) {
                        String childPath = getElementPath(child, fhirVersion);
                        if (childPath != null && !keyElements.contains(childPath)) {
                            keyElements.add(childPath);
                            newElements.add(childPath);
                        }
                    }
                }

                elementsToProcess = newElements;
            }
        } catch (Exception e) {
            logger.debug("Error getting key elements", e);
        }

        return keyElements;
    }

    /**
     * Step A: Build seed set (root + mustSupport + differential + ancestors).
     */
    private Set<String> buildSeedSet(IBaseResource structureDefinition) {
        Set<String> seedSet = new HashSet<>();

        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();

            // Get elements (differential preferred, snapshot fallback)
            List<IBase> elements = getAllElements(structureDefinition);

            // Add root element
            String rootPath = getRootPath(structureDefinition);
            if (rootPath != null) {
                seedSet.add(rootPath);
            }

            // Process each element
            for (IBase element : elements) {
                String path = getElementPath(element, fhirVersion);
                if (path == null) {
                    continue;
                }

                // Add all elements to seed set
                seedSet.add(path);

                // If element has mustSupport, also add all ancestors
                Boolean mustSupport = getElementMustSupport(element, fhirVersion);
                if (Boolean.TRUE.equals(mustSupport)) {
                    seedSet.addAll(getAncestorPaths(path));
                }
            }
        } catch (Exception e) {
            logger.debug("Error building seed set for StructureDefinition", e);
        }

        return seedSet;
    }

    /**
     * Step B: Get child elements that should be expanded based on constraints.
     */
    private List<IBase> getChildElementsForExpansion(IBaseResource structureDefinition, String parentPath) {
        List<IBase> childrenToExpand = new ArrayList<>();

        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            List<IBase> allElements = getAllElements(structureDefinition);

            for (IBase element : allElements) {
                String elementPath = getElementPath(element, fhirVersion);
                if (!isChildOf(elementPath, parentPath)) {
                    continue;
                }

                // Check expansion criteria
                if (shouldExpandChild(element, fhirVersion)) {
                    childrenToExpand.add(element);
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting child elements for expansion", e);
        }

        return childrenToExpand;
    }

    /**
     * Determines if a child element should be expanded based on Step B criteria.
     */
    private boolean shouldExpandChild(IBase element, FhirVersionEnum fhirVersion) {
        try {
            // Criterion 1: min != 0 (mandatory)
            Integer min = getElementMin(element, fhirVersion);
            if (min != null && min > 0) {
                return true;
            }

            // Criterion 2: max was constrained
            if (isMaxConstrained(element, fhirVersion)) {
                return true;
            }

            // Criterion 3: Participates in invariant (constraint)
            if (hasConstraints(element, fhirVersion)) {
                return true;
            }

            // Criterion 4: Is a slice
            if (isSlice(element, fhirVersion)) {
                return true;
            }

            // Criterion 5: Is a modifier element
            Boolean isModifier = getElementIsModifier(element, fhirVersion);
            if (Boolean.TRUE.equals(isModifier)) {
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error checking child expansion criteria", e);
        }

        return false;
    }

    /**
     * Step C: Extract bindings from key elements.
     */
    private Set<String> extractBindingsFromKeyElements(IBaseResource structureDefinition, Set<String> keyElements) {
        Set<String> valueSets = new HashSet<>();

        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            List<IBase> allElements = getAllElements(structureDefinition);

            for (IBase element : allElements) {
                String path = getElementPath(element, fhirVersion);
                if (path != null && keyElements.contains(path)) {
                    String valueSetUrl = getBindingValueSet(element, fhirVersion);
                    if (valueSetUrl != null && !valueSetUrl.isEmpty()) {
                        valueSets.add(valueSetUrl);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting bindings from key elements", e);
        }

        return valueSets;
    }

    /**
     * Gets the base definition for inheritance walking (Step D).
     */
    private IBaseResource getBaseDefinition(IBaseResource structureDefinition) {
        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(structureDefinition, "baseDefinition", IPrimitiveType.class);

            if (!results.isEmpty()) {
                String baseDefUrl = results.get(0).getValueAsString();
                if (baseDefUrl != null && !baseDefUrl.isEmpty()) {
                    // Try to resolve the base definition from repository
                    // For core resources, we might not have them, so return null
                    if (baseDefUrl.startsWith("http://hl7.org/fhir/StructureDefinition/")) {
                        // This is a core resource, stop inheritance walk
                        return null;
                    }

                    // Try to load from repository
                    try {
                        var id = Ids.newId(fhirVersion, "StructureDefinition", baseDefUrl);
                        return repository.read(structureDefinition.getClass(), id);
                    } catch (Exception e) {
                        logger.debug("Could not load base definition: {}", baseDefUrl);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting base definition", e);
        }

        return null;
    }

    // Helper methods for extracting information from ElementDefinition

    private List<IBase> getDifferentialElements(IBaseResource structureDefinition) {
        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            return fhirPath.evaluate(structureDefinition, "differential.element", IBase.class);
        } catch (Exception e) {
            logger.debug("Error getting differential elements", e);
            return new ArrayList<>();
        }
    }

    private List<IBase> getAllElements(IBaseResource structureDefinition) {
        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            // Try differential first, fall back to snapshot
            List<IBase> elements = fhirPath.evaluate(structureDefinition, "differential.element", IBase.class);
            if (elements.isEmpty()) {
                elements = fhirPath.evaluate(structureDefinition, "snapshot.element", IBase.class);
            }
            return elements;
        } catch (Exception e) {
            logger.debug("Error getting all elements", e);
            return new ArrayList<>();
        }
    }

    private String getRootPath(IBaseResource structureDefinition) {
        try {
            FhirVersionEnum fhirVersion = structureDefinition.getStructureFhirVersionEnum();
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(structureDefinition, "type", IPrimitiveType.class);
            if (!results.isEmpty()) {
                return results.get(0).getValueAsString();
            }
        } catch (Exception e) {
            logger.debug("Error getting root path", e);
        }
        return null;
    }

    private String getElementPath(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "path", IPrimitiveType.class);
            if (!results.isEmpty()) {
                return results.get(0).getValueAsString();
            }
        } catch (Exception e) {
            logger.debug("Error getting element path", e);
        }
        return null;
    }

    private Boolean getElementMustSupport(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "mustSupport", IPrimitiveType.class);
            if (!results.isEmpty()) {
                return (Boolean) results.get(0).getValue();
            }
        } catch (Exception e) {
            logger.debug("Error getting mustSupport", e);
        }
        return null;
    }

    private Integer getElementMin(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "min", IPrimitiveType.class);
            if (!results.isEmpty()) {
                Object value = results.get(0).getValue();
                if (value instanceof Integer) {
                    return (Integer) value;
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
            }
        } catch (Exception e) {
            logger.debug("Error getting min cardinality", e);
        }
        return null;
    }

    private Boolean getElementIsModifier(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "isModifier", IPrimitiveType.class);
            if (!results.isEmpty()) {
                return (Boolean) results.get(0).getValue();
            }
        } catch (Exception e) {
            logger.debug("Error getting isModifier", e);
        }
        return null;
    }

    private String getBindingValueSet(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "binding.valueSet", IPrimitiveType.class);
            if (!results.isEmpty()) {
                return results.get(0).getValueAsString();
            }
        } catch (Exception e) {
            logger.debug("Error getting binding valueSet", e);
        }
        return null;
    }

    private boolean isMaxConstrained(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "max", IPrimitiveType.class);
            if (!results.isEmpty()) {
                String max = results.get(0).getValueAsString();
                // If max is not "*", it's been constrained
                return max != null && !max.equals("*");
            }
        } catch (Exception e) {
            logger.debug("Error checking max constraint", e);
        }
        return false;
    }

    private boolean hasConstraints(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "constraint", IBase.class);
            return !results.isEmpty();
        } catch (Exception e) {
            logger.debug("Error checking constraints", e);
        }
        return false;
    }

    private boolean isSlice(IBase element, FhirVersionEnum fhirVersion) {
        try {
            var fhirPath = FhirContext.forCached(fhirVersion).newFhirPath();
            var results = fhirPath.evaluate(element, "sliceName", IPrimitiveType.class);
            if (!results.isEmpty()) {
                String sliceName = results.get(0).getValueAsString();
                return sliceName != null && !sliceName.isEmpty();
            }
        } catch (Exception e) {
            logger.debug("Error checking if element is slice", e);
        }
        return false;
    }

    private Set<String> getAncestorPaths(String path) {
        Set<String> ancestors = new HashSet<>();
        if (path == null || !path.contains(".")) {
            return ancestors;
        }

        String[] parts = path.split("\\.");
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                current.append(".");
            }
            current.append(parts[i]);
            ancestors.add(current.toString());
        }

        return ancestors;
    }

    private boolean isChildOf(String childPath, String parentPath) {
        if (childPath == null || parentPath == null) {
            return false;
        }

        // Direct child check: parent.child but not parent.child.grandchild
        if (childPath.startsWith(parentPath + ".")) {
            String remainder = childPath.substring(parentPath.length() + 1);
            return !remainder.contains(".");
        }

        return false;
    }
}
