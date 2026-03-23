package org.opencds.cqf.fhir.cr.measure.common;

/**
 * Composite key used to store and retrieve function evaluation results.
 *
 * <p>During measure evaluation, CQL function results (measure observations, non-subject-value
 * stratifiers) are stored in the {@link org.opencds.cqf.cql.engine.execution.EvaluationResult}
 * under a synthetic expression name that combines the criteria population ID with the function
 * expression name. This record replaces the ad-hoc string concatenation
 * ({@code popId + "-" + expression}) with a typed key.
 *
 * @param populationId the ID of the criteria population (e.g., numerator, denominator)
 * @param expression   the CQL function expression name
 */
public record FunctionResultKey(String populationId, String expression) {

    /**
     * Returns the string form used as the expression name in {@code EvaluationResult}.
     */
    public String toExpressionName() {
        return populationId + "-" + expression;
    }
}
