package org.opencds.cqf.fhir.cr.measure.r4;

import org.hl7.fhir.r4.model.MeasureReport;

/**
 * Interface for {@link R4MeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface R4MeasureEvaluatorSingle {

    MeasureReport evaluate(R4MeasureEvaluatorSingleRequest request);
}
