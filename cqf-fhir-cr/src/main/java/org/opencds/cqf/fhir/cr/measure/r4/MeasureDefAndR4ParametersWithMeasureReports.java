package org.opencds.cqf.fhir.cr.measure.r4;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.def.report.MeasureReportDef;

/**
 * Multi-measure evaluation result containing MeasureDefs and Parameters with bundled MeasureReports.
 *
 * <p><strong>TEST INFRASTRUCTURE ONLY - DO NOT USE IN PRODUCTION CODE</strong></p>
 *
 * <p>This record is used by R4 multi-measure test frameworks to assert on:</p>
 * <ul>
 *   <li><strong>measureDefs</strong>: List of pre-scoring internal state for all evaluated measures</li>
 *   <li><strong>parameters</strong>: Parameters resource containing bundled MeasureReports</li>
 * </ul>
 *
 * <p>Unlike {@link MeasureDefAndR4MeasureReport} which pairs a single MeasureDef with a single
 * MeasureReport, this record supports multi-measure evaluation where:</p>
 * <ul>
 *   <li>Multiple measures are evaluated (one MeasureDef per measure)</li>
 *   <li>MeasureReports are bundled in Parameters based on evaluation type:
 *     <ul>
 *       <li><strong>Population/SubjectList</strong>: ONE bundle with all MeasureReports</li>
 *       <li><strong>Patient/Subject</strong>: ONE bundle PER SUBJECT with their MeasureReports</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> Assumes synchronous, single-threaded evaluation.
 * MeasureDefs are mutable and safe only because test assertions run after evaluation completes.</p>
 *
 * @param measureDefs List of populated MeasureDefs after processResults (mutable references)
 * @param parameters Parameters resource containing bundled R4 MeasureReports
 */
@VisibleForTesting
public record MeasureDefAndR4ParametersWithMeasureReports(List<MeasureReportDef> measureDefs, Parameters parameters) {}
