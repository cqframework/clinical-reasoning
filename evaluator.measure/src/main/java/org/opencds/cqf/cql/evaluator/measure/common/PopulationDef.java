package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PopulationDef {

    protected MeasurePopulationType measurePopulationType;
    protected List<Object> evaluatedResources;
    protected List<Object> resources;
    protected Set<String> subjects;

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

    public List<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new ArrayList<>();
        }

        return this.evaluatedResources;
    }

    public Set<String> getSubjects() {
        if (this.subjects == null) {
            this.subjects = new HashSet<>();
        }

        return this.subjects;
    }

    public List<Object> getResources() {
        if (this.resources == null) {
            this.resources = new ArrayList<>();
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
