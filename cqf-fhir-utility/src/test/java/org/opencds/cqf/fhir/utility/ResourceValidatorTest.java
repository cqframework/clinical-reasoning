package org.opencds.cqf.fhir.utility;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class ResourceValidatorTest {

    @Test
    void constructorWithExternalValidator() {
        FhirValidator mockValidator = mock(FhirValidator.class);

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, null);

        assertNotNull(resourceValidator);
    }

    @Test
    void constructorWithExternalValidatorAndProfiles() {
        FhirValidator mockValidator = mock(FhirValidator.class);
        Map<String, ValidationProfile> profiles = new HashMap<>();
        profiles.put("test", new ValidationProfile("test-profile", Collections.emptyList()));

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, profiles);

        assertNotNull(resourceValidator);
    }

    @Test
    void validateWithExternalValidatorReturnsResourceOnSuccess() {
        FhirValidator mockValidator = mock(FhirValidator.class);
        ValidationResult mockResult = mock(ValidationResult.class);
        Patient patient = new Patient();

        when(mockValidator.validateWithResult(any(IBaseResource.class))).thenReturn(mockResult);
        when(mockResult.getMessages()).thenReturn(Collections.emptyList());

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, null);
        IBaseResource result = resourceValidator.validate(patient);

        assertSame(patient, result);
        verify(mockValidator).validateWithResult(patient);
    }

    @Test
    void validateWithExternalValidatorReturnsOperationOutcomeOnError() {
        FhirValidator mockValidator = mock(FhirValidator.class);
        ValidationResult mockResult = mock(ValidationResult.class);
        OperationOutcome operationOutcome = new OperationOutcome();
        Patient patient = new Patient();

        var errorMessage = new ca.uhn.fhir.validation.SingleValidationMessage();
        errorMessage.setSeverity(ca.uhn.fhir.validation.ResultSeverityEnum.ERROR);
        errorMessage.setMessage("Test validation error");

        when(mockValidator.validateWithResult(any(IBaseResource.class))).thenReturn(mockResult);
        when(mockResult.getMessages()).thenReturn(Collections.singletonList(errorMessage));
        when(mockResult.toOperationOutcome()).thenReturn(operationOutcome);

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, null);
        IBaseResource result = resourceValidator.validate(patient, false);

        assertSame(operationOutcome, result);
    }

    @Test
    void implementsIResourceValidatorInterface() {
        FhirValidator mockValidator = mock(FhirValidator.class);

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, null);

        assertTrue(resourceValidator instanceof IResourceValidator);
    }

    @Test
    void validateOverloadWithoutErrorFlagDelegatesToFullMethod() {
        FhirValidator mockValidator = mock(FhirValidator.class);
        ValidationResult mockResult = mock(ValidationResult.class);
        Patient patient = new Patient();

        when(mockValidator.validateWithResult(any(IBaseResource.class))).thenReturn(mockResult);
        when(mockResult.getMessages()).thenReturn(Collections.emptyList());

        ResourceValidator resourceValidator = new ResourceValidator(mockValidator, null);
        IBaseResource result = resourceValidator.validate(patient);

        assertSame(patient, result);
    }
}
