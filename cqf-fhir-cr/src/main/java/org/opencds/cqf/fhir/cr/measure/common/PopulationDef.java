package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulationDef {

    private static final Logger logger = LoggerFactory.getLogger(PopulationDef.class);

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

    public void retainAllResources(String subjectId, PopulationDef otherPopulationDef) {
        getResourcesForSubject(subjectId).retainAll(otherPopulationDef.getResourcesForSubject(subjectId));
    }

    public void retainAllSubjects(PopulationDef otherPopulationDef) {
        this.getSubjects().retainAll(otherPopulationDef.getSubjects());
    }

    public void removeAllResources(String subjectId, PopulationDef otherPopulationDef) {
        getResourcesForSubject(subjectId).removeAll(otherPopulationDef.getResourcesForSubject(subjectId));
    }

    private static List<String> printResourceIds(Collection<Object> resources) {
        return resources.stream()
                .filter(IBaseResource.class::isInstance)
                .map(IBaseResource.class::cast)
                .map(IBaseResource::getIdElement)
                .map(IIdType::getIdPart)
                .toList();
    }

    public void removeAllSubjects(PopulationDef otherPopulationDef) {
        this.getSubjects().removeAll(otherPopulationDef.getSubjects());
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

    public Set<Object> getResourcesForSubject(String subjectId) {
        return subjectResources.getOrDefault(subjectId, new HashSetForFhirResourcesAndCqlTypes<>());
    }

    // Add an element to Set<Object> under a key (Creates a new set if key is missing)
    public void addResource(String key, Object value) {
        subjectResources
                .computeIfAbsent(key, k -> new HashSetForFhirResourcesAndCqlTypes<>())
                .add(value);
    }

    public void removeOverlaps(String subjectId, PopulationDef otherPopulationDef) {

        var iterator = subjectResources.entrySet().iterator();
        var overlaps = otherPopulationDef.getSubjectResources();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<Object>> entry = iterator.next();
            String key = entry.getKey();
            // LUKETODO:  optimize this pattern:
            if (key.equals(subjectId)) {
                continue;
            }
            Set<Object> valuesInA = entry.getValue();

            if (overlaps.containsKey(key)) {
                valuesInA.removeAll(overlaps.get(key)); // Remove overlapping elements
            }

            if (valuesInA.isEmpty()) {
                iterator.remove(); // Safely remove key if Set is empty
            }
        }
    }

    public void retainOverlaps(String subjectId, PopulationDef otherPopulationDef) {
        var iterator = subjectResources.entrySet().iterator();
        var overlaps = otherPopulationDef.getSubjectResources();
        while (iterator.hasNext()) {
            Map.Entry<String, Set<Object>> entry = iterator.next();
            String key = entry.getKey();
            // LUKETODO:  why does this make the test fail?
            //            if (key.equals(subjectId)) {
            //                continue;
            //            }
            Set<Object> values = entry.getValue();

            if (overlaps.containsKey(key)) {
                // Retain only values also present in filterMap
                values.retainAll(overlaps.get(key));
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
