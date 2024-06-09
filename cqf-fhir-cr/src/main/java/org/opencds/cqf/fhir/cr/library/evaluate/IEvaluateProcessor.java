package org.opencds.cqf.fhir.cr.library.evaluate;

import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface IEvaluateProcessor {
    public IBaseParameters evaluate(EvaluateRequest request);
}
