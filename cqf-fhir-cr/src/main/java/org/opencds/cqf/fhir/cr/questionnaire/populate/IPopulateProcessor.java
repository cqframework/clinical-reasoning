package org.opencds.cqf.fhir.cr.questionnaire.populate;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IPopulateProcessor extends IOperationProcessor {
    IBaseResource populate(PopulateRequest request);
}
