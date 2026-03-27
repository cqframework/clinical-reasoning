package org.opencds.cqf.fhir.cr.measure.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SdeDef {

    private final String id;
    private final ConceptDef code;
    private final String expression;
    private final String description;
    private final Map<String, CriteriaResult> results = new HashMap<>();

    // Pre-accumulated state (populated by MeasureMultiSubjectEvaluator)
    private final Map<StratumValueWrapper, Long> accumulatedValues = new HashMap<>();
    private final Set<Object> allEvaluatedResources = new HashSet<>();

    public SdeDef(String id, ConceptDef code, String expression) {
        this(id, code, expression, null);
    }

    public SdeDef(String id, ConceptDef code, String expression, String description) {
        this.id = id;
        this.code = code;
        this.expression = expression;
        this.description = description;
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

    public String description() {
        return this.description;
    }

    public void putResult(String subject, Object value, Set<Object> evaluatedResources) {
        this.results.put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<StratumValueWrapper, Long> getAccumulatedValues() {
        return this.accumulatedValues;
    }

    public Set<Object> getAllEvaluatedResources() {
        return this.allEvaluatedResources;
    }

    /**
     * Aggregates per-subject SDE results into value counts and a merged set of evaluated resources.
     * Called by {@link MeasureMultiSubjectEvaluator#postEvaluationMultiSubject} for population reports.
     */
    public void accumulate() {
        // Merge all evaluated resources across subjects
        for (CriteriaResult result : results.values()) {
            allEvaluatedResources.addAll(result.evaluatedResources());
        }

        // Count occurrences of each distinct value across all subjects
        Map<StratumValueWrapper, Long> counts = results.values().stream()
                .flatMap(result -> result.nonNullValues().stream())
                .map(StratumValueWrapper::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        accumulatedValues.putAll(counts);
    }
}
