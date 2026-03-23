package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;

/**
 * Evaluation result pairing a {@link MeasureDef} (internal model) with its scored
 * {@link MeasureReport} (FHIR R4 resource).
 *
 * <p>Used by tests to assert on both pre-scoring internal state and the post-scoring FHIR
 * resource. Also used internally by {@code R4MultiMeasureService} during report building.</p>
 *
 * <p><strong>Thread Safety:</strong> Assumes synchronous, single-threaded evaluation.
 * MeasureDef is mutable and safe only because assertions run after evaluation completes.</p>
 *
 * @param measureDef The populated MeasureDef after processResults (mutable reference)
 * @param measureReport The scored R4 MeasureReport FHIR resource
 */
public record MeasureDefAndR4MeasureReport(
        MeasureDef measureDef, MeasureEvaluationState state, MeasureReport measureReport) {}
