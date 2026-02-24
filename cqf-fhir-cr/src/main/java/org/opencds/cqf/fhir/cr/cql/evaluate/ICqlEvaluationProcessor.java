package org.opencds.cqf.fhir.cr.cql.evaluate;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface ICqlEvaluationProcessor extends IOperationProcessor {
    IBaseParameters evaluate(CqlEvaluationRequest request);
}
