package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
    private final String description;

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis,
            List<SupportingEvidenceDef> supportingEvidenceDefs) {
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
        this(
                id,
                code,
                measurePopulationType,
                expression,
                populationBasis,
                criteriaReference,
                aggregateMethod,
                supportingEvidenceDefs,
                null);
    }

    public PopulationDef(
            String id,
            ConceptDef code,
            MeasurePopulationType measurePopulationType,
            String expression,
            CodeDef populationBasis,
            @Nullable String criteriaReference,
            @Nullable ContinuousVariableObservationAggregateMethod aggregateMethod,
            @Nullable List<SupportingEvidenceDef> supportingEvidenceDefs,
            @Nullable String description) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
        this.populationBasis = populationBasis;
        this.criteriaReference = criteriaReference;
        this.aggregateMethod = aggregateMethod;
        this.supportingEvidenceDefs = supportingEvidenceDefs;
        this.description = description;
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

    public boolean hasPopulationType(MeasurePopulationType populationType) {
        return populationType == this.measurePopulationType;
    }

    @Nullable
    public String getCriteriaReference() {
        return this.criteriaReference;
    }

    public String expression() {
        return this.expression;
    }

    @Nullable
    public ContinuousVariableObservationAggregateMethod getAggregateMethod() {
        return this.aggregateMethod;
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

    @Nullable
    public String description() {
        return this.description;
    }
}
