package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PopulationDef {

    private final String id;
    private final String expression;
    private final ConceptDef code;
    private final MeasurePopulationType measurePopulationType;
    // Added by Claude Sonnet 4.5 - for better encapsulation in getCountForScoring()
    private final CodeDef populationBasis;

    @Nullable
    private final String criteriaReference;

    @Nullable
    private final ContinuousVariableObservationAggregateMethod aggregateMethod;

    protected Set<Object> evaluatedResources;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis) {
        this(id, code, measurePopulationType, expression, populationBasis, null, null);
    }

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis,
            @Nullable String criteriaReference,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
        this.populationBasis = populationBasis;
        this.criteriaReference = criteriaReference;
        this.aggregateMethod = aggregateMethod;
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
     * <p/>
     * example:
     * population:
     * <Subject1,<Organization/1>>
     * <Subject2,<Organization/1>>
     * Population Count for Population Basis Organization = 2, even though the resulting resource object is the same
     * <Subject1,<1/1/2024>>
     * <Subject2,<1/1/2024>>
     * Population Count for Population Basis date = 2, even though the resulting resource object is the same
     *
     */
    public List<Object> getAllSubjectResources() {
        return subjectResources.values().stream()
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    // Extracted from R4MeasureReportBuilder.countObservations() by Claude Sonnet 4.5
    public int countObservations() {
        if (this.getAllSubjectResources() == null) {
            return 0;
        }

        return this.getAllSubjectResources().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .mapToInt(Map::size)
                .sum();
    }

    @Nullable
    public String getCriteriaReference() {
        return this.criteriaReference;
    }

    public String expression() {
        return this.expression;
    }

    // Added by Claude Sonnet 4.5 - getter for populationBasis field
    public CodeDef getPopulationBasis() {
        return this.populationBasis;
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

    @Nullable
    public ContinuousVariableObservationAggregateMethod getAggregateMethod() {
        return this.aggregateMethod;
    }

    // Added by Claude Sonnet 4.5 - unified count retrieval for scoring
    /**
     * Returns the count for this population based on the population basis.
     * For boolean basis, returns the number of subjects.
     * For measure observations, returns the count of observations.
     * For resource basis, returns the count of all subject resources.
     *
     * @return the count to use for scoring
     */
    public int getCountForScoring() {
        if (populationBasis.code().equals("boolean")) {
            return getSubjects().size();
        } else if (type().equals(MeasurePopulationType.MEASUREOBSERVATION)) {
            return countObservations();
        } else {
            return getAllSubjectResources().size();
        }
    }
}
