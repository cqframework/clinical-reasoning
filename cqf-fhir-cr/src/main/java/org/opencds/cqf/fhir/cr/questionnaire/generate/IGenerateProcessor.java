package org.opencds.cqf.fhir.cr.questionnaire.generate;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IGenerateProcessor {
    IBaseResource generate(String id);

    IBaseResource generate(GenerateRequest request, String id);

    IBaseBackboneElement generateItem(GenerateRequest request);
}
