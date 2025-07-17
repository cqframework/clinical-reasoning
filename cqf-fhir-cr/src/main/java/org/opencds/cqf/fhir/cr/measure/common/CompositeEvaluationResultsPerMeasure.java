package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

/**
 * Container meant to hold the results or an early and cached CQL measure evaluation that holds
 * two data points:
 * <ol>
 * <li>To hold the results of a measure evaluation, grouped by measure ID, with each measure evaluation grouped by Subject ID</li>
 * <li>To hold any errors that occurred during the evaluation, grouped by measure ID</li>
 * </ol>
 * These data points are not mutually exclusive, meaning a measure may have both successful results and errors.
 * <p/>
 * This class also allows the caller to mutate a {@link MeasureDef} with the errors that occurred during the evaluation
 */
public class CompositeEvaluationResultsPerMeasure {
    // The same measure may have successful results AND errors, so account for both
    private final Map<IIdType, Map<String, EvaluationResult>> resultsPerMeasure;
    // We may get several errors for a given measure
    private final Map<IIdType, List<String>> errorsPerMeasure;

    private CompositeEvaluationResultsPerMeasure(Builder builder) {

        var resultsBuilder = ImmutableMap.<IIdType, Map<String, EvaluationResult>>builder();
        builder.resultsPerMeasure.forEach((key, value) -> resultsBuilder.put(key, ImmutableMap.copyOf(value)));
        resultsPerMeasure = resultsBuilder.build();

        var errorsBuilder = ImmutableMap.<IIdType, List<String>>builder();
        builder.errorsPerMeasure.forEach((key, value) -> errorsBuilder.put(key, List.copyOf(value)));
        errorsPerMeasure = errorsBuilder.build();
    }

    /**
     * Retrieves results and populates errors for a given measure.
     * This method uses direct map lookups for efficient data retrieval.
     * measureDef will occasionally be prepended with the version, which means we need to parse it into an IIdType which
     * is too much work, so pass in the measureId directly
     *
     * @param measureId the ID of the measure to process
     * @param measureDef the MeasureDef to populate with errors
     *
     * @return a map of evaluation results per subject, or an empty map if none exist
     */
    public Map<String, EvaluationResult> processMeasureForSuccessOrFailure(IIdType measureId, MeasureDef measureDef) {
        var unqualifiedMeasureId = measureId.toUnqualifiedVersionless();

        errorsPerMeasure.getOrDefault(unqualifiedMeasureId, List.of()).forEach(measureDef::addError);

        // We are explicitly maintaining the logic of accepting the lack of any sort of results,
        // either errors or successes, and returning an empty map.
        return resultsPerMeasure.getOrDefault(unqualifiedMeasureId, Map.of());
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

        public void addResults(List<IIdType> measureIds, String subjectId, EvaluationResult evaluationResult) {
            for (IIdType measureId : measureIds) {
                addResult(measureId, subjectId, evaluationResult);
            }
        }

        public void addResult(IIdType measureId, String subjectId, EvaluationResult evaluationResult) {
            var resultPerMeasure =
                    resultsPerMeasure.computeIfAbsent(measureId.toUnqualifiedVersionless(), k -> new HashMap<>());

            resultPerMeasure.put(subjectId, evaluationResult);
        }

        public void addErrors(List<? extends IIdType> measureIds, String error) {
            if (error == null || error.isEmpty()) {
                return;
            }

            for (IIdType measureId : measureIds) {
                addError(measureId, error);
            }
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
