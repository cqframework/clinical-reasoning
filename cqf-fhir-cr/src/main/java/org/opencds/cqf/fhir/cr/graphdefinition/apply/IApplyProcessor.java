package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;

public interface IApplyProcessor extends IOperationProcessor {
    IBaseResource apply(ApplyRequest request);
}
