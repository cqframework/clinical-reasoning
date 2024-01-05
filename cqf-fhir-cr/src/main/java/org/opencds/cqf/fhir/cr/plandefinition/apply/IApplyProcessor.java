package org.opencds.cqf.fhir.cr.plandefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IApplyProcessor {
    IBaseResource apply(ApplyRequest request);

    IBaseBundle applyR5(ApplyRequest request);
}
