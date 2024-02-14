package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IApplyProcessor {
    public IBaseResource apply(ApplyRequest request);
}
