package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StratifierComponentDef {
    private final String id;
    private final ConceptDef code;
    private final String expression;

    private Map<String, CriteriaResult> results;

    public StratifierComponentDef(String id, ConceptDef code, String expression) {
        this.id = id;
        this.code = code;
        this.expression = expression;
    }

    public String id() {
        return this.id;
    }

    public String expression() {
        return this.expression;
    }

    public ConceptDef code() {
        return this.code;
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

    /**
     * Creates a shallow copy snapshot of this StratifierComponentDef.
     * <p>
     * Copies the results map. All other fields are immutable.
     *
     * @return A new StratifierComponentDef instance with copied results map
     */
    public StratifierComponentDef createSnapshot() {
        // Create new instance
        StratifierComponentDef snapshot = new StratifierComponentDef(id, code, expression);

        // Deep copy results map (CriteriaResult is immutable)
        if (this.results != null) {
            snapshot.results = new HashMap<>(this.results);
        }
        // If null, leave snapshot.results as null (lazy init preserved)

        return snapshot;
    }
}
