package org.opencds.cqf.fhir.cr.measure.common;

import java.util.List;

/**
 * Thrown when a stratifier expression result type is not valid for stratification
 * (e.g. a list of FHIR resources instead of a scalar value).
 */
public class InvalidStratifierExpressionTypeException extends RuntimeException {
    public InvalidStratifierExpressionTypeException(
            String expression,
            String populationBasisCode,
            String measureUrl,
            List<String> resultClassNames,
            List<String> matchingClassNames) {
        super(("stratifier expression criteria results for expression: [%s] must fall within accepted types"
                        + " for population-basis: [%s] for Measure: [%s]"
                        + " due to mismatch between total eval result classes: %s and matching result classes: %s")
                .formatted(expression, populationBasisCode, measureUrl, resultClassNames, matchingClassNames));
    }
}
