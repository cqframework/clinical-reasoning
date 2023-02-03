package org.opencds.cqf.cql.evaluator.measure.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PopulationDef {

    private final String id;
    private final String expression;
    private final ConceptDef code;
    private final MeasurePopulationType measurePopulationType;

    protected Set<Object> evaluatedResources;
    protected Set<Object> resources;
    protected Set<String> subjects;

    public PopulationDef(String id, ConceptDef code, MeasurePopulationType measurePopulationType, String expression) {
        this.id = id;
        this.code = code;
        this.measurePopulationType = measurePopulationType;
        this.expression = expression;
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

    public void addEvaluatedResource(Object resource) {
        this.getEvaluatedResources().add(resource);
    }

    public Set<Object> getEvaluatedResources() {
        if (this.evaluatedResources == null) {
            this.evaluatedResources = new HashSet<>();
        }

        return this.evaluatedResources;
    }

    public void addSubject(String subject) {
        this.getSubjects().add(subject);
    }

    public void removeSubject(String subject) {
        this.getSubjects().remove(subject);
    }

    public Set<String> getSubjects() {
        if (this.subjects == null) {
            this.subjects = new HashSet<>();
        }

        return this.subjects;
    }

    public void addResource(Object resource) {
        this.getResources().add(resource);
    }

    public void removeResource(Object resource) {
        this.getResources().remove(resource);
    }

    public Set<Object> getResources() {
        if (this.resources == null) {
            this.resources = new HashSet<>();
        }

        return this.resources;
    }

    public String expression() {
        return this.expression;
    }
}
