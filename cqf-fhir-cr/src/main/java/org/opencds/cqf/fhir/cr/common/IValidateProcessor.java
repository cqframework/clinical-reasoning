package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

public interface IValidateProcessor extends IOperationProcessor {

    IBaseOperationOutcome validate(IBaseBundle resource, String mode, String profile);
}
