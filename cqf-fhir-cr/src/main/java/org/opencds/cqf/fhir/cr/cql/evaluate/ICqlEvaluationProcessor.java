package org.opencds.cqf.fhir.cr.cql.evaluate;

import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface ICqlEvaluationProcessor {
    IBaseParameters evaluate(CqlEvaluationRequest request);
}
