package org.opencds.cqf.fhir.cr.library.evaluate;

import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IEvaluateProcessor extends IOperationProcessor {
    public IBaseParameters evaluate(EvaluateRequest request);
}
