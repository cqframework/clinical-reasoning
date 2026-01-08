package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
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
    private final CodeDef populationBasis;
    private final List<SupportingEvidenceDef> supportingEvidenceDefs;

    @Nullable
    private final String criteriaReference;

    @Nullable
    private final ContinuousVariableObservationAggregateMethod aggregateMethod;

    @Nullable
    private Double aggregationResult;

    protected Set<Object> evaluatedResources;
    protected Map<String, Set<Object>> subjectResources = new HashMap<>();

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis,
            @Nullable List<SupportingEvidenceDef> supportingEvidenceDefs) {
        this(id, code, measurePopulationType, expression, populationBasis, null, null, supportingEvidenceDefs);
    }

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis,
            @Nullable String criteriaReference,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable List<SupportingEvidenceDef> supportingEvidenceDefs) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
        this.populationBasis = populationBasis;
        this.criteriaReference = criteriaReference;
        this.aggregateMethod = aggregateMethod;
        this.supportingEvidenceDefs = supportingEvidenceDefs;
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

    /**
     * Get the population basis code for this population.
     * The population basis determines how population members are counted.
     *
     * @return the population basis CodeDef
     */
    public CodeDef getPopulationBasis() {
        return this.populationBasis;
    }

    /**
     * Check if this population uses boolean basis (patient-based counting).
     * When true, counts unique subjects. When false, counts all resources.
     *
     * @return true if population basis is "boolean", false otherwise
     */
    public boolean isBooleanBasis() {
        return this.populationBasis.code().equals("boolean");
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

    @Nullable
    public Double getAggregationResult() {
        return aggregationResult;
    }

    public void setAggregationResult(@Nullable Double aggregationResult) {
        this.aggregationResult = aggregationResult;
    }

    /**
     * Added by Claude Sonnet 4.5 on 2025-12-02
     * Updated by Claude Sonnet 4.5 on 2025-12-08 to use own populationBasis instead of GroupDef parameter.
     * Get the count for this population based on its type and population basis.
     * This is the single source of truth for population counts.
     *
     * @return the count for this population
     */
    public int getCount() {
        // For MEASUREOBSERVATION populations, count the observations
        if (this.measurePopulationType == MeasurePopulationType.MEASUREOBSERVATION) {
            return countObservations();
        }

        // For other population types, use population basis to determine count
        if (isBooleanBasis()) {
            // Boolean basis: count unique subjects
            return getSubjects().size();
        } else {
            // Non-boolean basis: count all resources (including duplicates across subjects)
            return getAllSubjectResources().size();
        }
    }

    @Override
    public String toString() {
        String codeText = (code != null && code.text() != null) ? code.text() : "null";
        String criteriaRef = (criteriaReference != null) ? criteriaReference : "null";
        String aggMethod = (aggregateMethod != null) ? aggregateMethod.toString() : "null";

        return "PopulationDef{"
                + "id='" + id + '\''
                + ", code.text='" + codeText + '\''
                + ", type=" + measurePopulationType
                + ", expression='" + expression + '\''
                + ", criteriaReference='" + criteriaRef + '\''
                + ", aggregateMethod=" + aggMethod
                + '}';
    }

    public List<SupportingEvidenceDef> getSupportingEvidenceDefs() {
        return supportingEvidenceDefs == null ? null : new ArrayList<>(supportingEvidenceDefs);
    }
}
