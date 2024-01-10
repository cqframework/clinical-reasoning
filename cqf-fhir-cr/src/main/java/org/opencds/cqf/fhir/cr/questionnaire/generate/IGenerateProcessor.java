package org.opencds.cqf.fhir.cr.questionnaire.generate;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public interface IGenerateProcessor {
    IBaseResource generate(String id);

    IBaseResource generate(ICpgRequest request, IBaseResource profile, String id);

    IBaseBackboneElement generateItem(ICpgRequest request, IBaseResource profile, int itemCount);
}
