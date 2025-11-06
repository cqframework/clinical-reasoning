package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public void removeAllSubjects(PopulationDef otherPopulationDef) {
        this.getSubjects().removeAll(otherPopulationDef.getSubjects());
    }

    /**
     * Used if we want to count all resources that may be duplicated across subjects, for example,
     * for Date values that will be identical across subjects, but we want to count the duplicates.
     */
    public List<Object> getResourcesDuplicatesAcrossSubjects() {
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
}
