package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

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
        var existingResult = getResults().get(subject);

        if (existingResult == null) {
            getResults().put(subject, new CriteriaResult(value, evaluatedResources));
            return;
        }

        if (value instanceof Map<?, ?> newMapAny && existingResult.rawValue() instanceof Map<?, ?> existingMapAny) {
            Map<Object, Object> merged =
                    getObjectObjectMap(subject, (Map<Object, Object>) newMapAny, (Map<Object, Object>) existingMapAny);

            Set<Object> mergedEvaluated = new HashSet<>();
            if (existingResult.evaluatedResources() != null)
                mergedEvaluated.addAll(existingResult.evaluatedResources());
            if (evaluatedResources != null) mergedEvaluated.addAll(evaluatedResources);

            getResults().put(subject, new CriteriaResult(merged, mergedEvaluated));
            return;
        }

        // Non-map: overwrite (warn of collisions)
        getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    private static @NotNull Map<Object, Object> getObjectObjectMap(
            String subject, Map<Object, Object> newMapAny, Map<Object, Object> existingMapAny) {

        Map<Object, Object> merged = new HashMap<>(existingMapAny);

        for (Map.Entry<Object, Object> e : newMapAny.entrySet()) {
            Object key = e.getKey();
            Object newVal = e.getValue();

            merged.merge(key, newVal, (oldVal, incomingVal) -> resolveCollision(subject, key, oldVal, incomingVal));
        }
        return merged;
    }

    private static Object resolveCollision(String subject, Object key, Object existingValue, Object newValue) {
        if (java.util.Objects.equals(existingValue, newValue)) {
            return existingValue;
        }

        // Common policy: prefer non-null
        if (existingValue == null) return newValue;
        if (newValue == null) return existingValue;

        // Collisions: deterministic + observable
        org.slf4j.LoggerFactory.getLogger(StratifierComponentDef.class)
                .warn(
                        "mergeResult collision for subject '{}' key='{}': existingValue='{}', newValue='{}'. Keeping existingValue.",
                        subject,
                        key,
                        existingValue,
                        newValue);

        return existingValue; // or return newValue if you want "last wins"
    }

    public Map<String, CriteriaResult> getResults() {
        if (this.results == null) {
            this.results = new HashMap<>();
        }

        return this.results;
    }
}
