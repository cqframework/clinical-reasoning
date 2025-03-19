package org.opencds.cqf.fhir.cr.measure.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PopulationDef {

    private final String id;
    private final String expression;
    private final ConceptDef code;
    private final MeasurePopulationType measurePopulationType;

    protected Set<Object> evaluatedResources;
    protected Set<Object> resources;
    protected Set<String> subjects;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public PopulationDef(String id, ConceptDef code, MeasurePopulationType measurePopulationType, String expression) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
    }

    public MeasurePopulationType type() {
        return this.measurePopulationType;
    }

    public String id() {
        return this.id;
    }

    public ConceptDef code() {
        return this.code;
    }

    public void addEvaluatedResource(Object resource) {
        this.getEvaluatedResources().add(resource);
    }

    public Set<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new HashSet<>();
        }

        return this.evaluatedResources;
    }

    public void addSubject(String subject) {
        this.getSubjects().add(subject);
    }

    public void removeSubject(String subject) {
        this.getSubjects().remove(subject);
    }

    public Set<String> getSubjects() {
        if (this.subjects == null) {
            this.subjects = new HashSet<>();
        }

        return this.subjects;
    }

    public void addResource(Object resource) {
        this.getResources().add(resource);
    }

    public void removeResource(Object resource) {
        this.getResources().remove(resource);
    }

    public Set<Object> getResources() {
        if (this.resources == null) {
            this.resources = new HashSet<>();
        }

        return this.resources;
    }

    public String expression() {
        return this.expression;
    }

    // Getter method
    public Map<String, Set<Object>> getSubjectResources() {
        return subjectResources;
    }

    // Setter method
    public void setSubjectResources(Map<String, Set<Object>> subjectResources) {
        this.subjectResources = subjectResources;
    }

    // ✅ Get Set<Object> by key (Returns an empty set if key is missing)
    public Set<Object> getResourcesByKey(String key) {
        return subjectResources.getOrDefault(key, Collections.emptySet());
    }

    // ✅ Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources.computeIfAbsent(key, k -> new HashSet<>()).add(value);
    }

    // ✅ Remove an element from Set<Object> under a key (Removes key if set is empty)
    public boolean removeResource(String key, Object value) {
        Set<Object> resources = subjectResources.get(key);
        if (resources != null) {
            boolean removed = resources.remove(value);
            if (resources.isEmpty()) {
                subjectResources.remove(key); // Clean up empty keys
            }
            return removed;
        }
        return false; // Key does not exist
    }
    // ✅ Remove a specific object from the Set without removing the entire Set
    public boolean removeObjectFromSet(String key, Object value) {
        Set<Object> resources = subjectResources.get(key);
        if (resources != null) {
            return resources.remove(value); // Returns true if object was in the set and removed, false otherwise
        }
        return false; // Key does not exist or object was not found
    }

    public void removeOverlaps(Map<String, Set<Object>> overlap) {
        var iterator = subjectResources.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Set<Object>> entry = iterator.next();
            String key = entry.getKey();
            Set<Object> valuesInA = entry.getValue();

            if (overlap.containsKey(key)) {
                valuesInA.removeAll(overlap.get(key)); // Remove overlapping elements
            }

            if (valuesInA.isEmpty()) {
                iterator.remove(); // Safely remove key if Set is empty
            }
        }
    }
}
