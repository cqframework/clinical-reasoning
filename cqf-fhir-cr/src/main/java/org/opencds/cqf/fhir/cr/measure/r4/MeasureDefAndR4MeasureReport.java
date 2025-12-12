package org.opencds.cqf.fhir.cr.measure.r4;

import com.google.common.annotations.VisibleForTesting;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;

/**
 * Evaluation result containing both MeasureDef (internal model) and
 * MeasureReport (FHIR R4 resource).
 *
 * <p><strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong></p>
 *
 * <p>This record is used by R4 test frameworks to assert on both:</p>
 * <ul>
 *   <li><strong>MeasureDef</strong>: Pre-scoring internal state</li>
 *   <li><strong>MeasureReport</strong>: Post-scoring FHIR resource</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Assumes synchronous, single-threaded evaluation.
 * MeasureDef is mutable and safe only because test assertions run after evaluation completes.</p>
 *
 * @param measureDef The populated MeasureDef after processResults (mutable reference)
 * @param measureReport The scored R4 MeasureReport FHIR resource
 */
@VisibleForTesting
public record MeasureDefAndR4MeasureReport(MeasureDef measureDef, MeasureReport measureReport) {}
