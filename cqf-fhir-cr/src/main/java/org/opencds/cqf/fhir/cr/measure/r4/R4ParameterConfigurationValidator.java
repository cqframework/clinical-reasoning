package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.repository.IRepository;
import java.util.HashSet;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDefValidationContext;
import org.opencds.cqf.fhir.cr.measure.common.ParameterConfigurationValidator;
import org.opencds.cqf.fhir.cr.measure.common.ValidationIssue;
import org.opencds.cqf.fhir.cr.measure.common.ValidationResult;
import org.opencds.cqf.fhir.cr.measure.common.ValidationSeverity;
import org.opencds.cqf.fhir.utility.search.Searches;

/**
 * R4-specific parameter configuration validator. Reads parameter definitions from the Library
 * resource and validates that required parameters are provided and that no unknown parameters
 * are passed. Produces {@code MISSING_REQUIRED_PARAMETER} errors and {@code UNKNOWN_PARAMETER} warnings.
 */
public class R4ParameterConfigurationValidator extends ParameterConfigurationValidator {

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();
        var measure = (Measure) context.measure();

        if (!measure.hasLibrary() || measure.getLibrary().isEmpty()) {
            return result;
        }

        var url = measure.getLibrary().get(0).asStringValue();
        var bundle = context.repository().search(Bundle.class, Library.class, Searches.byCanonical(url), null);

        if (bundle.getEntry().isEmpty()) {
            return result;
        }

        var library = (Library) bundle.getEntryFirstRep().getResource();
        validateLibraryParameters(library, context.parameters(), context.repository(), result);

        return result;
    }

    @Override
    protected void validateLibraryParameters(
            IBaseResource libraryResource,
            Map<String, Object> parameters,
            IRepository repository,
            ValidationResult result) {

        var library = (Library) libraryResource;
        if (!library.hasParameter()) {
            return;
        }

        // Collect defined parameter names (input parameters only)
        var definedInputParams = new HashSet<String>();
        for (ParameterDefinition paramDef : library.getParameter()) {
            if (paramDef.getUse() == ParameterDefinition.ParameterUse.IN) {
                definedInputParams.add(paramDef.getName());

                // Check required parameters (min > 0) are present
                if (paramDef.getMin() > 0 && !parameters.containsKey(paramDef.getName())) {
                    result.addIssue(new ValidationIssue(
                            ValidationSeverity.ERROR,
                            MISSING_REQUIRED_PARAMETER,
                            "Required parameter '%s' (type: %s) is not provided"
                                    .formatted(paramDef.getName(), paramDef.getType()),
                            "Provide the required parameter '%s' of type '%s' in the operation request."
                                    .formatted(paramDef.getName(), paramDef.getType())));
                }
            }
        }

        // Check for unknown parameters (excluding well-known operation parameters)
        var wellKnownParams = java.util.Set.of("Measurement Period");
        for (var paramName : parameters.keySet()) {
            if (!definedInputParams.contains(paramName) && !wellKnownParams.contains(paramName)) {
                result.addIssue(new ValidationIssue(
                        ValidationSeverity.WARNING,
                        UNKNOWN_PARAMETER,
                        "Parameter '%s' is not defined in the Library's parameter definitions".formatted(paramName),
                        "Check that the parameter name '%s' matches a parameter defined in the CQL library."
                                .formatted(paramName)));
            }
        }
    }
}
