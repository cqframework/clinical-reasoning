package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * Runs an ordered list of {@link MeasureDefValidator} implementations and merges their results
 * into a single {@link ValidationResult}. All validators are executed regardless of earlier failures,
 * so the caller receives the complete set of issues in one pass.
 */
public class CompositeMeasureDefValidator implements MeasureDefValidator {

    private final List<MeasureDefValidator> validators;

    public CompositeMeasureDefValidator(List<MeasureDefValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    @Override
    public ValidationResult validate(MeasureDefValidationContext context) {
        var result = new ValidationResult();
        for (var validator : validators) {
            result.merge(validator.validate(context));
        }
        return result;
    }
}
