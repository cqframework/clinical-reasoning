package org.opencds.cqf.fhir.cr.measure.common.def.measure;

import jakarta.annotation.Nullable;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.def.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;

/**
 * Immutable definition of a FHIR Measure Population structure.
 * Contains only the population's structural metadata (id, code, type, expression, basis, criteria reference, aggregate method).
 * Does NOT contain evaluation state like evaluated resources or subject resources - use PopulationReportDef for that.
 */
public class PopulationDef {

    private final String id;
    private final String expression;
    private final ConceptDef code;
    private final MeasurePopulationType measurePopulationType;
    private final CodeDef populationBasis;

    @Nullable
    private final String criteriaReference;

    @Nullable
    private final ContinuousVariableObservationAggregateMethod aggregateMethod;

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

    public String expression() {
        return this.expression;
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

    @Nullable
    public String getCriteriaReference() {
        return this.criteriaReference;
    }

    @Nullable
    public ContinuousVariableObservationAggregateMethod getAggregateMethod() {
        return this.aggregateMethod;
    }
}
