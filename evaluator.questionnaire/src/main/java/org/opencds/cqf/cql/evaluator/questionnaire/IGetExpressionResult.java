package org.opencds.cqf.cql.evaluator.questionnaire;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface IGetExpressionResult {
    IBase evaluate(String expression, String language, String libraryToBeEvaluated, IBaseParameters params);
}
