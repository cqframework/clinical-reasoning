package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateProcessor implements IValidateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ValidateProcessor.class);
    private final FhirContext fhirContext;

    public ValidateProcessor(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Override
    public IBaseOperationOutcome validate(IBaseBundle bundle, String mode, String profile) {
        logger.info("Unable to perform CRMI $validate outside of HAPI context");
        return (IBaseOperationOutcome) fhirContext.getResourceDefinition("OperationOutcome")
            .newInstance();
    }
}
