package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    private Map<StratumValueWrapper, Long> accumulatedValues;
    private Set<Object> allEvaluatedResources;

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
        this.getResults().put(subject, new CriteriaResult(value, evaluatedResources));
    }

    public Map<String, CriteriaResult> getResults() {
        return this.results;
    }

    public void setAccumulatedValues(Map<StratumValueWrapper, Long> accumulatedValues) {
        this.accumulatedValues = accumulatedValues;
    }

    public Map<StratumValueWrapper, Long> getAccumulatedValues() {
        return this.accumulatedValues != null ? this.accumulatedValues : Collections.emptyMap();
    }

    public void setAllEvaluatedResources(Set<Object> allEvaluatedResources) {
        this.allEvaluatedResources = allEvaluatedResources;
    }

    public Set<Object> getAllEvaluatedResources() {
        return this.allEvaluatedResources != null ? this.allEvaluatedResources : Collections.emptySet();
    }

    public boolean isAccumulated() {
        return this.accumulatedValues != null;
    }

    public boolean hasResults() {
        return getResults() != null && !getResults().isEmpty();
    }

    public void accumulate() {
        if (!hasResults()) {
            return;
        }

        // Aggregate evaluated resources across all subjects
        Set<Object> allEvaluatedResources = getResults().values().stream()
                .flatMap(criteriaResult -> criteriaResult.evaluatedResources().stream())
                .collect(Collectors.toSet());
        setAllEvaluatedResources(allEvaluatedResources);

        // Accumulate values by grouping with count
        Map<StratumValueWrapper, Long> accumulated = getResults().values().stream()
                .flatMap(criteriaResult -> Lists.newArrayList(criteriaResult.iterableValue()).stream())
                .filter(Objects::nonNull)
                .map(StratumValueWrapper::new)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        setAccumulatedValues(accumulated);
    }
}
