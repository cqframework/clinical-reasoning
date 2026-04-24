package org.opencds.cqf.fhir.cr.measure.r4;

import java.util.List;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReference;

/**
 * Interface for {@link R4MultiMeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface R4MeasureEvaluatorMultiple {

    Parameters evaluate(List<MeasureReference> measures, MeasureEvaluationRequest request, Parameters parameters);
}
