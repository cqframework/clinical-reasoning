package org.opencds.cqf.fhir.cr.group.evaluate;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IEvaluateProcessor extends IOperationProcessor {
    public <R extends IBaseResource> R evaluate(EvaluateRequest request);
}
