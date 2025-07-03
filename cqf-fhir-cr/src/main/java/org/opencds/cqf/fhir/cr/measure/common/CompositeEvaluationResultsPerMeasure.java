package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

// LUKETODO:  javadoc
// This is basically a Map of measure -> subject -> EvaluationResult
public class CompositeEvaluationResultsPerMeasure {
    // The same measure may have successful results AND errors, so account for both
    private final Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure;
    // We may get several errors for a given measure
    private final Map<IIdType, List<String>> errorsPerMeasure;

    private CompositeEvaluationResultsPerMeasure(Builder builder) {

        var resultsBuilder = ImmutableMap.<IIdType, Map<String, EvaluationResult>>builder();
        var errorsBuilder = ImmutableMap.<IIdType, List<String>>builder();

        builder.resultsPerMeasure.forEach((key, value) -> resultsBuilder.put(key, ImmutableMap.copyOf(value)));

        resultsPerMeasure = resultsBuilder.build();

        builder.errorsPerMeasure.forEach((key, value) -> errorsBuilder.put(key, List.copyOf(value)));

        errorsPerMeasure = errorsBuilder.build();
    }

    // measureDef will occasionally be prepended with the version, which means we need to parse it into an IIdType which
    // is too much work, so pass in the measureId directly
    public Map<String, EvaluationResult> processMeasureForSuccessOrFailure(IIdType measureId, MeasureDef measureDef) {
        var hasFoundErrors = errorsPerMeasure.entrySet().stream()
                .filter(entry -> isMeasureDefFound(entry.getKey(), measureId))
                .map(Entry::getValue)
                .flatMap(List::stream)
                .peek(measureDef::addError)
                .findAny()
                .isPresent();

        var resultForMeasure = resultsPerMeasure.entrySet().stream()
                .filter(entry -> isMeasureDefFound(entry.getKey(), measureId))
                .map(Entry::getValue)
                .findFirst();

        // We are explicitly maintaining the logic of accepting the lack of any sort of results
        // either errors or successes, and returning an empty map.
        return resultForMeasure.orElseGet(Map::of);
    }

    private boolean isMeasureDefFound(IIdType entryKey, IIdType measureId) {
        return measureId.toUnqualifiedVersionless().equals(entryKey.toUnqualifiedVersionless());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure = new HashMap<>();
        private final Map<IIdType, List<String>> errorsPerMeasure = new HashMap<>();

        public CompositeEvaluationResultsPerMeasure build() {
            return new CompositeEvaluationResultsPerMeasure(this);
        }

        public void addResult(IIdType measureId, String subjectId, EvaluationResult evaluationResult) {
            var resultPerMeasure =
                    resultsPerMeasure.computeIfAbsent(measureId.toUnqualifiedVersionless(), k -> new HashMap<>());

            resultPerMeasure.put(subjectId, evaluationResult);
        }

        public void addError(IIdType measureId, String error) {
            if (error == null || error.isBlank()) {
                return;
            }

            errorsPerMeasure
                    .computeIfAbsent(measureId.toUnqualifiedVersionless(), k -> new ArrayList<>())
                    .add(error);
        }
    }
}
