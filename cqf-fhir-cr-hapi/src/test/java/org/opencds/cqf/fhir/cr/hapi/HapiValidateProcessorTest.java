package org.opencds.cqf.fhir.cr.hapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cr.hapi.common.HapiValidateProcessor;
import org.opencds.cqf.fhir.cr.hapi.config.FhirValidatorRegistry;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class HapiValidateProcessorTest {

    public HapiValidateProcessor hapiValidateProcessor;

    @Mock
    FhirValidatorRegistry fhirValidatorRegistry;

    @Mock
    FhirValidator fhirValidator;

    @Test
    void validate_mode_param_throws_exception() {
        var ctx = FhirContext.forR4();
        hapiValidateProcessor = new HapiValidateProcessor(ctx, fhirValidatorRegistry);

        Bundle toValidate = new Bundle();

        Assertions.assertThrows(NotImplementedOperationException.class, () -> {
            hapiValidateProcessor.validate(toValidate, "mode", null);
        });
    }

    @Test
    void validate_profile_param_throws_exception() {
        var ctx = FhirContext.forR4();
        hapiValidateProcessor = new HapiValidateProcessor(ctx, fhirValidatorRegistry);

        Bundle toValidate = new Bundle();

        Assertions.assertThrows(NotImplementedOperationException.class, () -> {
            hapiValidateProcessor.validate(toValidate, null, "profile");
        });
    }

    @Test
    void validate_no_bundle_throws_exception() {
        var ctx = FhirContext.forR4();
        hapiValidateProcessor = new HapiValidateProcessor(ctx, fhirValidatorRegistry);

        Assertions.assertThrows(UnprocessableEntityException.class, () -> {
            hapiValidateProcessor.validate(null, null, null);
        });
    }

    @Test
    void validate_verify_operation() {
        var ctx = FhirContext.forR4();
        Bundle toValidate = new Bundle();

        when(fhirValidatorRegistry.getValidator(any())).thenReturn(fhirValidator);
        when(fhirValidator.validateWithResult(toValidate)).thenReturn(new ValidationResult(ctx, Collections.emptyList()));
        hapiValidateProcessor = new HapiValidateProcessor(ctx, fhirValidatorRegistry);

        hapiValidateProcessor.validate(toValidate, null, null);
        verify(fhirValidatorRegistry, times(1)).getValidator(any());
        verify(fhirValidator, times(1)).validateWithResult(toValidate);
    }

}
