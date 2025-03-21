package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.Bundle;

/**
 * Interface for {@link R4MultiMeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface R4MeasureEvaluatorMultiple {

    Bundle evaluate(R4MeasureEvaluatorMultipleRequest request);
}
