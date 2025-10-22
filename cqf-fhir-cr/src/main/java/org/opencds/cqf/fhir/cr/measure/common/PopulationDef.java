package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
    protected Set<Object> resources;
    protected Set<String> subjects;
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
            this.evaluatedResources = new HashSetForFhirResources<>();
        }

        return this.evaluatedResources;
    }

    public Set<String> getSubjects() {
        return this.getSubjectResources().keySet();
    }

    public void retainAllResources(Set<Object> resourcesToRetain) {
        var resourcesToRetainForDebug = resourcesToRetain.stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());
        var resourcesInDefForDebug = getResources().stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());

        final Iterator<Entry<String, Set<Object>>> entryIterator =
                getSubjectResources().entrySet().iterator();

        while (entryIterator.hasNext()) {
            final Set<Object> resourcesFromEntry = entryIterator.next().getValue();

            resourcesFromEntry.retainAll(resourcesToRetain);
        }

        var postRetainResourcesInDef = getResources().stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());
        logger.info(
                "resourcesInDef:\n{},\nresourcesToRetain:\n{},\npostRetainResourcesInDef:\n{}",
                resourcesInDefForDebug,
                resourcesToRetainForDebug,
                postRetainResourcesInDef);
    }

    public void retainAllSubjects(Set<String> subjects) {
        this.getSubjects().retainAll(subjects);
    }

    public void removeAllResources(Set<Object> resourcesToRemove) {
        var resourcesToRemoveForDebug = resourcesToRemove.stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());
        var resourcesInDefForDebug = getResources().stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());
        // LUKETODO:  I think this is too aggressive, even though the tests seem to pass
        getSubjectResources().entrySet().removeIf(entry -> containsResourceForRemove(resourcesToRemove, entry));
        var postRemoveResourcesInDef = getResources().stream()
                .map(res -> (res instanceof IBaseResource base)
                        ? base.getIdElement().getValueAsString()
                        : "?")
                .collect(Collectors.toSet());
        logger.info(
                "resourcesInDef:\n{},\nresourcesToRemove:\n{},\npostRemoveResourcesInDef:\n{}",
                resourcesInDefForDebug,
                resourcesToRemoveForDebug,
                postRemoveResourcesInDef);
    }

    private boolean containsResourceForRetain(Set<Object> resources, Entry<String, Set<Object>> entry) {
        final HashSetForFhirResources<Object> resourcesToTestForRetain = new HashSetForFhirResources<>(resources);

        final Set<Object> resourcesInEntry = entry.getValue();

        for (Object resourceInEntry : resourcesInEntry) {
            if (resourcesToTestForRetain.contains(resourceInEntry)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsResourceForRemove(Set<Object> resources, Entry<String, Set<Object>> entry) {
        final Set<Object> resourcesInEntry = entry.getValue();

        for (Object resource : resources) {
            if (resourcesInEntry.contains(resource)) {
                return true;
            }
        }

        return false;
    }

    public void removeAllSubjects(Set<String> subjects) {
        this.getSubjects().removeAll(subjects);
    }

    public Set<Object> getResources() {
        return new HashSetForFhirResources<>(subjectResources.values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet()));
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
                .computeIfAbsent(key, k -> new HashSetForFhirResources<>())
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
