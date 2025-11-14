package org.opencds.cqf.fhir.cr.plandefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IApplyProcessor extends IOperationProcessor {
    IBaseResource apply(ApplyRequest request);

    IBaseBundle applyR5(ApplyRequest request);
}
