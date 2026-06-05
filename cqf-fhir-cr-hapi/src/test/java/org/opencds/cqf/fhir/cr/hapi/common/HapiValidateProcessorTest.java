package org.opencds.cqf.fhir.cr.hapi.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.util.ClasspathUtil;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import java.util.Collections;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cr.CrSettings;

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
        when(fhirValidator.validateWithResult(toValidate))
                .thenReturn(new ValidationResult(ctx, Collections.emptyList()));
        hapiValidateProcessor = new HapiValidateProcessor(ctx, fhirValidatorRegistry);

        hapiValidateProcessor.validate(toValidate, null, null);
        verify(fhirValidatorRegistry, times(1)).getValidator(any());
        verify(fhirValidator, times(1)).validateWithResult(toValidate);
    }

    @Test
    void validate_verify_operation_outcome_no_errors() {
        var ctx = FhirContext.forR4();
        Bundle toValidate = ClasspathUtil.loadResource(
                ctx, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/r4/Bundle-validation-no-errors.json");

        FhirValidatorRegistry registry = new FhirValidatorRegistry(null, CrSettings.getDefault());
        hapiValidateProcessor = new HapiValidateProcessor(ctx, registry);

        OperationOutcome result = (OperationOutcome) hapiValidateProcessor.validate(toValidate, null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                0,
                result.getIssue().stream()
                        .filter(i -> i.getSeverity().equals(IssueSeverity.ERROR))
                        .toList()
                        .size());
    }

    @Test
    void validate_verify_operation_outcome_errors() {
        var ctx = FhirContext.forR4();
        Bundle toValidate = ClasspathUtil.loadResource(
                ctx, Bundle.class, "org/opencds/cqf/fhir/cr/hapi/r4/Bundle-validation-errors.json");

        FhirValidatorRegistry registry = new FhirValidatorRegistry(null, CrSettings.getDefault());
        hapiValidateProcessor = new HapiValidateProcessor(ctx, registry);

        OperationOutcome result = (OperationOutcome) hapiValidateProcessor.validate(toValidate, null, null);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(
                2,
                result.getIssue().stream()
                        .filter(i -> i.getSeverity().equals(IssueSeverity.ERROR))
                        .toList()
                        .size());
    }
}
