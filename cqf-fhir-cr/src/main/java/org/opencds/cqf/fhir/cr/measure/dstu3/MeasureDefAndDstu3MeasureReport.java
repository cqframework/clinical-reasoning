package org.opencds.cqf.fhir.cr.measure.dstu3;

import com.google.common.annotations.VisibleForTesting;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Evaluation result containing both MeasureReportDef (internal model with evaluation state) and
 * MeasureReport (FHIR DSTU3 resource).
 *
 * <p><strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong></p>
 *
 * <p>This record is used by DSTU3 test frameworks to assert on both:</p>
 * <ul>
 *   <li><strong>MeasureDef</strong>: Immutable measure structure (via measureDef())</li>
 *   <li><strong>MeasureReportDef</strong>: Evaluation results and internal state</li>
 *   <li><strong>MeasureReport</strong>: Scored FHIR resource</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Assumes synchronous, single-threaded evaluation.
 * MeasureReportDef is mutable and safe only because test assertions run after evaluation completes.</p>
 *
 * @param measureReportDef The populated MeasureReportDef after evaluation (mutable reference)
 * @param measureReport The scored DSTU3 MeasureReport FHIR resource
 */
@VisibleForTesting
public record MeasureDefAndDstu3MeasureReport(MeasureReportDef measureReportDef, MeasureReport measureReport) {

    /**
     * Convenience method to access the immutable MeasureDef structure.
     * Delegates to measureReportDef.measureDef().
     */
    public org.opencds.cqf.fhir.cr.measure.common.def.measure.MeasureDef measureDef() {
        return measureReportDef.measureDef();
    }
}
