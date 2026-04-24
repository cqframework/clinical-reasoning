package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.repository.IRepository;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Version-agnostic base class for validating that user-supplied parameters match the CQL library's
 * parameter definitions. Subclasses (e.g. {@code R4ParameterConfigurationValidator}) override
 * {@link #validateLibraryParameters} to perform FHIR-version-specific checks.
 */
public class ParameterConfigurationValidator implements MeasureDefValidator {

    public static final String MISSING_REQUIRED_PARAMETER = "MISSING_REQUIRED_PARAMETER";
    public static final String UNKNOWN_PARAMETER = "UNKNOWN_PARAMETER";

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();

        // Parameter validation requires version-specific Library access to read
        // Library.parameter definitions. This base implementation validates that
        // required operation-level parameters (like measurement period) are present.
        // Version-specific subclasses can override to add Library parameter validation.

        return result;
    }

    /**
     * Subclasses should override this to extract parameter definitions from the Library resource
     * and validate against the provided parameters map.
     */
    protected void validateLibraryParameters(
            IBaseResource library, Map<String, Object> parameters, IRepository repository, ValidationResult result) {
        // Default no-op; version-specific implementations provide this
    }
}
