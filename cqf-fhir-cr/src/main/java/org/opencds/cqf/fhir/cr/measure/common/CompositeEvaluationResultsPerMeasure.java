package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

// LUKETODO:  javadoc
public class CompositeEvaluationResultsPerMeasure {
    private final Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure;

    private CompositeEvaluationResultsPerMeasure(Builder builder) {
        this.resultsPerMeasure = builder.resultsPerMeasure.entrySet()
            .stream()
            .collect(ImmutableMap.toImmutableMap(
                Map.Entry::getKey,
                entry -> ImmutableMap.copyOf(entry.getValue())
            ));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, EvaluationResult> getResultForMeasure(IIdType measureId) {
        return resultsPerMeasure.getOrDefault(measureId, Map.of());
    }

    public static class Builder {
        private final Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure = new HashMap<>();

        public CompositeEvaluationResultsPerMeasure build() {
            return new CompositeEvaluationResultsPerMeasure(this);
        }

        public void addResult(IIdType measureId, String subjectId, EvaluationResult evaluationResult) {
            var resultPerMeasure =
                resultsPerMeasure.computeIfAbsent( measureId.toUnqualifiedVersionless(), k -> new HashMap<>());

            resultPerMeasure.put(subjectId, evaluationResult);
        }
    }
}
