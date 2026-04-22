package org.opencds.cqf.fhir.cr.measure.r4;

import com.google.common.annotations.VisibleForTesting;
import java.util.Map;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

/**
 * Evaluation result containing MeasureDef (internal model), MeasureReport (FHIR R4 resource),
 * and the raw CQL EvaluationResult objects keyed by subject ID.
 *
 * <p>The {@code evaluationResults} map holds one {@link EvaluationResult} per evaluated subject.
 * When debug logging is enabled on the CQL engine, each result's
 * {@link EvaluationResult#getDebugResult()} contains per-library, per-expression debug entries
 * and {@link EvaluationResult#getTrace()} contains the execution trace.</p>
 *
 * @param measureDef The populated MeasureDef after processResults (mutable reference)
 * @param measureReport The scored R4 MeasureReport FHIR resource
 * @param evaluationResults The raw CQL evaluation results per subject ID
 */
@VisibleForTesting
public record MeasureDefAndR4MeasureReport(
        MeasureDef measureDef, MeasureReport measureReport, Map<String, EvaluationResult> evaluationResults) {

    /**
     * Backwards-compatible constructor for callers that do not need evaluation results.
     */
    public MeasureDefAndR4MeasureReport(MeasureDef measureDef, MeasureReport measureReport) {
        this(measureDef, measureReport, Map.of());
    }
}
