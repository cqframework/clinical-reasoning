package org.opencds.cqf.fhir.cr.questionnaire.generate;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public interface IElementProcessor {
    IBaseBackboneElement processElement(
            IOperationRequest request,
            ICompositeType element,
            String profileUrl,
            String childLinkId,
            IBaseResource caseFeature);
}
