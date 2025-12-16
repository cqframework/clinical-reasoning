package org.opencds.cqf.fhir.cr.measure.common.def.report;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.def.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.def.measure.SdeDef;

public class SdeReportDef {

    private final SdeDef sdeDef; // Reference to immutable structure
    private Map<String, CriteriaResult> results; // Evaluation-only state

    /**
     * Factory method to create SdeReportDef from immutable SdeDef.
     * The SdeReportDef will have empty mutable state (results map).
     */
    public static SdeReportDef fromSdeDef(SdeDef sdeDef) {
        return new SdeReportDef(sdeDef);
    }

    /**
     * Constructor for creating SdeReportDef with a SdeDef reference.
     * This is the primary constructor for production use.
     */
    public SdeReportDef(SdeDef sdeDef) {
        this.sdeDef = sdeDef;
    }

    /**
     * Test-only constructor for creating SdeReportDef with explicit structural data.
     * Creates a minimal SdeDef internally. Use fromSdeDef() for production code.
     */
    public SdeReportDef(String id, ConceptDef code, String expression) {
        this.sdeDef = new SdeDef(id, code, expression);
    }

    /**
     * Accessor for the immutable structural definition.
     */
    public SdeDef sdeDef() {
        return this.sdeDef;
    }

    // Delegate structural queries to sdeDef
    public String id() {
        return sdeDef.id();
    }

    public String expression() {
        return sdeDef.expression();
    }

    public ConceptDef code() {
        return sdeDef.code();
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
