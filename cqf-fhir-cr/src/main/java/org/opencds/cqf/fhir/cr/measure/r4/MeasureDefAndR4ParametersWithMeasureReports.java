package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.List;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationState;

/**
 * Multi-measure evaluation result pairing {@link MeasureDef}s with a {@link Parameters}
 * resource containing bundled {@link org.hl7.fhir.r4.model.MeasureReport}s.
 *
 * <p>Used by tests to assert on both pre-scoring internal state and the post-scoring FHIR
 * resource. Also used internally by {@code R4MultiMeasureService} during report packaging.</p>
 *
 * <p>Unlike {@link MeasureDefAndR4MeasureReport} which pairs a single MeasureDef with a single
 * MeasureReport, this record supports multi-measure evaluation where multiple measures produce
 * MeasureReports bundled in Parameters based on evaluation type.</p>
 *
 * <p><strong>Thread Safety:</strong> Assumes synchronous, single-threaded evaluation.
 * MeasureDefs are mutable and safe only because assertions run after evaluation completes.</p>
 *
 * @param measureDefs List of populated MeasureDefs after processResults (mutable references)
 * @param parameters Parameters resource containing bundled R4 MeasureReports
 */
public record MeasureDefAndR4ParametersWithMeasureReports(
        List<MeasureDef> measureDefs, List<MeasureEvaluationState> states, Parameters parameters) {}
