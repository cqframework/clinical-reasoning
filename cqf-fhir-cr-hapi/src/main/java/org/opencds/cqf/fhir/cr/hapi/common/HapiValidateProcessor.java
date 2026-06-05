package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.opencds.cqf.fhir.cr.common.IValidateProcessor;

public class HapiValidateProcessor implements IValidateProcessor {

    private final FhirValidator fhirValidator;

    public HapiValidateProcessor(FhirContext fhirContext, FhirValidatorRegistry fhirValidatorRegistry) {
        this.fhirValidator =
                fhirValidatorRegistry.getValidator(fhirContext.getVersion().getVersion());
    }

    @Override
    public IBaseOperationOutcome validate(IBaseBundle bundle, String mode, String profile) {
        if (mode != null) {
            throw new NotImplementedOperationException("'mode' Parameter is not implemented yet");
        }
        if (profile != null) {
            throw new NotImplementedOperationException("'profile' Parameter is not implemented yet");
        }
        if (bundle == null) {
            throw new UnprocessableEntityException("A FHIR bundle must be provided for validation");
        }

        return fhirValidator.validateWithResult(bundle).toOperationOutcome();
    }
}
