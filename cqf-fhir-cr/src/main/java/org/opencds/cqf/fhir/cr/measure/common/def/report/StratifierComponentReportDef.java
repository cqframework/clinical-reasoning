package org.opencds.cqf.fhir.cr.measure.common.def.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.StratifierComponentDef;

public class StratifierComponentReportDef {
    private final StratifierComponentDef componentDef; // Reference to immutable structure
    private Map<String, CriteriaResult> results; // Evaluation-only state

    /**
     * Factory method to create StratifierComponentReportDef from immutable StratifierComponentDef.
     * The StratifierComponentReportDef will have empty mutable state (results map).
     */
    public static StratifierComponentReportDef fromStratifierComponentDef(StratifierComponentDef componentDef) {
        return new StratifierComponentReportDef(componentDef);
    }

    /**
     * Constructor for creating StratifierComponentReportDef with a StratifierComponentDef reference.
     * This is the primary constructor for production use.
     */
    public StratifierComponentReportDef(StratifierComponentDef componentDef) {
        this.componentDef = componentDef;
    }

    /**
     * Test-only constructor for creating StratifierComponentReportDef with explicit structural data.
     * Creates a minimal StratifierComponentDef internally. Use fromStratifierComponentDef() for production code.
     */
    public StratifierComponentReportDef(String id, ConceptDef code, String expression) {
        this.componentDef = new StratifierComponentDef(id, code, expression);
    }

    /**
     * Accessor for the immutable structural definition.
     */
    public StratifierComponentDef componentDef() {
        return this.componentDef;
    }

    // Delegate structural queries to componentDef
    public String id() {
        return componentDef.id();
    }

    public String expression() {
        return componentDef.expression();
    }

    public ConceptDef code() {
        return componentDef.code();
    }

    public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
        this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }
}
