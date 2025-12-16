package org.opencds.cqf.fhir.cr.measure.common;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Container meant to hold the results or an early and cached CQL measure evaluation that holds
 * two data points:
 * <ol>
 * <li>To hold the results of a measure evaluation, grouped by measure ID, with each measure evaluation grouped by Subject ID</li>
 * <li>To hold any errors that occurred during the evaluation, grouped by measure ID</li>
 * </ol>
 * These data points are not mutually exclusive, meaning a measure may have both successful results and errors.
 * <p/>
 * This class also allows the caller to mutate a {@link MeasureReportDef} with the errors that occurred during the evaluation
 */
public class CompositeEvaluationResultsPerMeasure {
    // The same measure may have successful results AND errors, so account for both
    private final Map<MeasureReportDef, Map<String, EvaluationResult>> resultsPerMeasure;
    // We may get several errors for a given measure
    private final Map<MeasureReportDef, List<String>> errorsPerMeasure;

    private CompositeEvaluationResultsPerMeasure(Builder builder) {

        var resultsBuilder = ImmutableMap.<MeasureReportDef, Map<String, EvaluationResult>>builder();
        builder.resultsPerMeasure.forEach((key, value) -> resultsBuilder.put(key, ImmutableMap.copyOf(value)));
        resultsPerMeasure = resultsBuilder.build();

        var errorsBuilder = ImmutableMap.<MeasureReportDef, List<String>>builder();
        builder.errorsPerMeasure.forEach((key, value) -> errorsBuilder.put(key, List.copyOf(value)));
        errorsPerMeasure = errorsBuilder.build();
    }

    /**
     * Retrieves results and populates errors for a given measure.
     * This method uses direct map lookups for efficient data retrieval.
     *
     * @param measureDef the MeasureDef to populate with errors
     *
     * @return a map of evaluation results per subject, or an empty map if none exist
     */
    public Map<String, EvaluationResult> processMeasureForSuccessOrFailure(MeasureReportDef measureDef) {
        errorsPerMeasure.getOrDefault(measureDef, List.of()).forEach(measureDef::addError);

        // We are explicitly maintaining the logic of accepting the lack of any sort of results,
        // either errors or successes, and returning an empty map.
        return resultsPerMeasure.getOrDefault(measureDef, Map.of());
    }

    /**
     * Expose method to allow retrieval of evaluated cql results per Measure.
     * IIdType for Measure is key, Nested {@code Map<String, EvaluationResult>} has Key for subject evaluated,
     * and associated EvaluationResult produced from CQL expression evaluation
     * @return {@code Map<IIdType, Map<String, EvaluationResult>>}
     */
    public Map<MeasureReportDef, Map<String, EvaluationResult>> getResultsPerMeasure() {
        return this.resultsPerMeasure;
    }

    /**
     * Expose method to allow retrieval of captured errors produced from evaluated cql per Measure.
     * When an error is produced while evaluating, we capture the errors generated in this object, which can be rendered per Measure evaluated.
     * @return {@code Map<IIdType, List<String>>}
     */
    public Map<MeasureReportDef, List<String>> getErrorsPerMeasure() {
        return this.errorsPerMeasure;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<MeasureReportDef, Map<String, EvaluationResult>> resultsPerMeasure = new HashMap<>();
        private final Map<MeasureReportDef, List<String>> errorsPerMeasure = new HashMap<>();

        public CompositeEvaluationResultsPerMeasure build() {
            return new CompositeEvaluationResultsPerMeasure(this);
        }

        public void addResults(
                List<MeasureReportDef> measureDefs,
                String subjectId,
                EvaluationResult evaluationResult,
                List<EvaluationResult> measureObservationResults) {
            for (MeasureReportDef measureDef : measureDefs) {
                addResult(measureDef, subjectId, evaluationResult, measureObservationResults);
            }
        }

        public void addResult(
                MeasureReportDef measureDef,
                String subjectId,
                EvaluationResult evaluationResult,
                List<EvaluationResult> measureObservationResults) {

            // if we have no results, we don't need to add anything
            if (evaluationResult == null
                    || evaluationResult.getExpressionResults().isEmpty()) {
                return;
            }

            var evaluationResultToUse = mergeEvaluationResults(evaluationResult, measureObservationResults);

            var resultPerMeasure = resultsPerMeasure.computeIfAbsent(measureDef, k -> new HashMap<>());

            resultPerMeasure.put(subjectId, evaluationResultToUse);
        }

        public void addErrors(List<MeasureReportDef> measureDefs, String error) {
            if (error == null || error.isEmpty()) {
                return;
            }

            for (MeasureReportDef measureDef : measureDefs) {
                addError(measureDef, error);
            }
        }

        public void addError(MeasureReportDef measureDef, String error) {
            if (error == null || error.isBlank()) {
                return;
            }

            errorsPerMeasure.computeIfAbsent(measureDef, k -> new ArrayList<>()).add(error);
        }

        private EvaluationResult mergeEvaluationResults(
                EvaluationResult origEvaluationResult, List<EvaluationResult> measureObservationResults) {
            final EvaluationResult evaluationResult = new EvaluationResult();

            var copyOfExpressionResults = new HashMap<>(origEvaluationResult.getExpressionResults());

            for (EvaluationResult measureObservationResult : measureObservationResults) {
                copyOfExpressionResults.putAll(measureObservationResult.getExpressionResults());
            }

            evaluationResult.getExpressionResults().putAll(copyOfExpressionResults);

            return evaluationResult;
        }
    }
}
