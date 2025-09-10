package org.opencds.cqf.fhir.cr.graphdefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IApplyProcessor {
    IBaseResource apply(ApplyRequest request);
}
