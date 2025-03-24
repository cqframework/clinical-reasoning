package org.opencds.cqf.fhir.cr.measure.common;

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

    // Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources.computeIfAbsent(key, k -> new HashSet<>()).add(value);
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

    public void retainOverlaps(Map<String, Set<Object>> filterMap) {
        var iterator = subjectResources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<Object>> entry = iterator.next();
            String key = entry.getKey();
            Set<Object> values = entry.getValue();

            if (filterMap.containsKey(key)) {
                // Retain only values also present in filterMap
                values.retainAll(filterMap.get(key));
            } else {
                // If the key doesn't exist in filterMap, remove the entire entry
                iterator.remove();
                continue;
            }

            // If no values remain, remove the key
            if (values.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
