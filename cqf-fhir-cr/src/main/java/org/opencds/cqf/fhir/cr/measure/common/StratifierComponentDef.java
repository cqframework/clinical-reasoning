package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.HashSet;
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

    /**
     * Merges function results for NON_SUBJECT_VALUE stratifiers.
     * When processing multiple populations, each population may have different resources
     * that need stratification. This method merges the Map&lt;inputResource, outputValue&gt;
     * results from different populations instead of overwriting them.
     *
     * @param subject the subject ID
     * @param value the function result (expected to be Map&lt;Object, Object&gt; for NON_SUBJECT_VALUE stratifiers)
     * @param evaluatedResources the resources evaluated during CQL execution
     */
    @SuppressWarnings("unchecked")
    public void mergeResult(String subject, Object value, Set<Object> evaluatedResources) {
        var existingResult = this.getResults().get(subject);

        if (existingResult == null) {
            // No existing result, just put the new one
            this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
            return;
        }

        // Merge Map results (for NON_SUBJECT_VALUE stratifiers with function expressions)
        if (value instanceof Map && existingResult.rawValue() instanceof Map) {
            Map<Object, Object> existingMap = (Map<Object, Object>) existingResult.rawValue();
            Map<Object, Object> newMap = (Map<Object, Object>) value;

            // Create merged map
            Map<Object, Object> mergedMap = new HashMap<>(existingMap);
            mergedMap.putAll(newMap);

            // Merge evaluated resources
            Set<Object> mergedEvaluatedResources = new HashSet<>(existingResult.evaluatedResources());
            mergedEvaluatedResources.addAll(evaluatedResources);

            this.getResults().put(subject, new CriteriaResult(mergedMap, mergedEvaluatedResources));
        } else {
            // Non-map values: just overwrite (shouldn't happen for NON_SUBJECT_VALUE stratifiers)
            this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
        }
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }
}
