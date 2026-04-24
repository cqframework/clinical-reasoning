package org.opencds.cqf.fhir.cr.measure.dstu3;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.MeasureReport;
import org.hl7.fhir.dstu3.model.Parameters;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvaluationRequest;

/**
 * Interface for {@link Dstu3MeasureService} and any other concrete classes that implement the same
 * signature.
 */
public interface Dstu3MeasureEvaluatorSingle {

    MeasureReport evaluateMeasure(IdType id, MeasureEvaluationRequest request, Parameters parameters);
}
