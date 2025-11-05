package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PopulationDef {

    private final String id;
    private final String expression;
    private final ConceptDef code;
    private final MeasurePopulationType measurePopulationType;

    @Nullable
    private final String criteriaReference;

    protected Set<Object> evaluatedResources;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public PopulationDef(String id, ConceptDef code, MeasurePopulationType measurePopulationType, String expression) {
        this(id, code, measurePopulationType, expression, null);
    }

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            @Nullable String criteriaReference) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
        this.criteriaReference = criteriaReference;
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

    public Set<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new HashSetForFhirResourcesAndCqlTypes<>();
        }

        return this.evaluatedResources;
    }

    public Set<String> getSubjects() {
        return this.getSubjectResources().keySet();
    }

    // LUKETODO:  add a subject ID to filter out the resources instead of doing this for all
    public void retainAllResources(Set<Object> resourcesToRetain) {
        getSubjectResources().forEach((key, value) -> value.retainAll(resourcesToRetain));
    }

    // LUKETODO:  add a subject ID to filter out the resources instead of doing this for all
    public void retainAllSubjects(Set<String> subjects) {
        this.getSubjects().retainAll(subjects);
    }

    // LUKETODO:  add a subject ID to filter out the resources instead of doing this for all
    public void removeAllResources(Set<Object> resourcesToRemove) {
        getSubjectResources().forEach((key, value) -> value.removeAll(resourcesToRemove));
    }

    // LUKETODO:  add a subject ID to filter out the resources instead of doing this for all
    public void removeAllSubjects(Set<String> subjects) {
        this.getSubjects().removeAll(subjects);
    }

    // LUKETODO:  is there any use case for this method at all, or should we remove it?
    public Set<Object> getResources() {
        return new HashSetForFhirResourcesAndCqlTypes<>(subjectResources.values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
    }

    public List<Object> getResourcesList() {
        return subjectResources.values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    public String getCriteriaReference() {
        return this.criteriaReference;
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
        subjectResources
                .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                .add(value);
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
