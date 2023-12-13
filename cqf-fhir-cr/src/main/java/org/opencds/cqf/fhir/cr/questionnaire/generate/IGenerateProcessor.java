package org.opencds.cqf.fhir.cr.questionnaire.generate;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public interface  IGenerateProcessor {
    IBaseResource generate(String id);

    IBaseResource generate(String id, IBaseResource profile);

    IBaseBackboneElement generateItem(IBaseResource profile, int itemCount);

    IBaseBackboneElement generateItem(IOperationRequest request, IBaseResource profile, int itemCount);
}
