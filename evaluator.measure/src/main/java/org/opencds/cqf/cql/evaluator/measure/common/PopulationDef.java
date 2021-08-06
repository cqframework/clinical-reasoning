package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.HashSet;
import java.util.Set;

public class PopulationDef {

    protected MeasurePopulationType measurePopulationType;
    protected Set<Object> evaluatedResources;
    protected Set<Object> resources;
    protected Set<Object> subjects;

    protected String criteriaExpression;


    public PopulationDef(MeasurePopulationType measurePopulationType, String criteriaExpression) {
        this.measurePopulationType = measurePopulationType;
        this.criteriaExpression = criteriaExpression;
    }


    public PopulationDef(MeasurePopulationType measurePopulationType) {
        this(measurePopulationType, null);
    }


    public MeasurePopulationType getType() {
        return this.measurePopulationType;
    }

    public Set<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new HashSet<>();
        }

        return this.evaluatedResources;
    }

    public Set<Object> getSubjects() {
        if (this.subjects == null) {
            this.subjects = new HashSet<>();
        }

        return this.subjects;
    }

    public Set<Object> getResources() {
        if (this.resources == null) {
            this.resources = new HashSet<>();
        }

        return this.resources;
    }

    public String getCriteriaExpression() {
        return this.criteriaExpression;
    }

    public void setCriteriaExpression(String criteriaExpression) {
        this.criteriaExpression = criteriaExpression;
    }
}
