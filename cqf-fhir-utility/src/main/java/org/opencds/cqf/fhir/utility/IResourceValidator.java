package org.opencds.cqf.fhir.utility;

import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Interface for FHIR resource validation. Implementations can use the HAPI FHIR validator
 * or any custom validation logic.
 */
public interface IResourceValidator {

    /**
     * Validates the given resource. Returns the resource if valid, or an OperationOutcome
     * containing validation errors if invalid.
     *
     * @param resource the resource to validate
     * @return the original resource if valid, or an OperationOutcome if invalid
     */
    IBaseResource validate(IBaseResource resource);

    /**
     * Validates the given resource.
     *
     * @param resource the resource to validate
     * @param throwOnError if true, throws a RuntimeException when validation fails;
     *                     if false, returns an OperationOutcome
     * @return the original resource if valid, or an OperationOutcome if invalid and throwOnError is false
     * @throws RuntimeException if validation fails and throwOnError is true
     */
    IBaseResource validate(IBaseResource resource, Boolean throwOnError);
}
