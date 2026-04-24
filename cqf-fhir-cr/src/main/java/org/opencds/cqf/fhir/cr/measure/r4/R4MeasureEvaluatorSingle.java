package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Interface to capture measure evaluation on a single measure.
 */
public interface R4MeasureEvaluatorSingle {

    MeasureReport evaluate(MeasureReference measure, MeasureEvaluationRequest request, Parameters parameters);
}
