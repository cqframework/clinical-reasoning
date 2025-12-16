package org.opencds.cqf.fhir.cr.measure.common;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure mathematical functions for measure scoring calculations.
 *
 * <p>This utility class provides building blocks for measure scoring that can be used by:
 * <ul>
 *   <li>Internal scorers (MeasureReportDefScorer) operating on Def objects</li>
 *   <li>External scorers (R4/R5MeasureReportScorer) operating on FHIR MeasureReport objects</li>
 *   <li>External clients building custom scoring or aggregation logic</li>
 * </ul>
 *
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Pure functions</strong> - Static methods with no side effects</li>
 *   <li><strong>Primitive values</strong> - Takes int, double, List&lt;QuantityDef&gt; as inputs</li>
 *   <li><strong>No framework dependencies</strong> - Independent of Def or FHIR structures</li>
 *   <li><strong>Single source of truth</strong> - One implementation per algorithm</li>
 * </ul>
 *
 * <p><strong>Scoring Algorithms:</strong>
 * <ul>
 *   <li><strong>Proportion/Ratio</strong>: {@code (n - nx) / (d - dx - de)}</li>
 *   <li><strong>Ratio Continuous Variable</strong>: {@code numeratorAggregate / denominatorAggregate}</li>
 *   <li><strong>Continuous Variable</strong>: Aggregates using SUM, AVG, MIN, MAX, MEDIAN, COUNT</li>
 * </ul>
 *
 * <p><strong>Future Evolution:</strong> This class can evolve to return value objects
 * (e.g., {@code ScoringResult}) that include intermediate calculation data for extensions.
 *
 * @see MeasureReportDefScorer
 * @see org.opencds.cqf.fhir.cr.measure.r4.R4MeasureReportScorer
 * @since 2025-12-16
 */
public class MeasureScoreCalculator {

    // Private constructor - utility class should not be instantiated
    private MeasureScoreCalculator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculate proportion/ratio score using the formula: {@code (n - nx) / (d - dx - de)}
     *
     * <p>Used for PROPORTION and RATIO scoring types (non-continuous-variable).
     *
     * <p><strong>Formula:</strong>
     * <pre>
     * effectiveNumerator = numerator - numeratorExclusion
     * effectiveDenominator = denominator - denominatorExclusion - denominatorException
     * score = effectiveNumerator / effectiveDenominator
     * </pre>
     *
     * <p><strong>Example:</strong>
     * <pre>
     * Given:
     *   numerator = 4, numeratorExclusion = 1
     *   denominator = 6, denominatorExclusion = 1, denominatorException = 1
     *
     * Calculation:
     *   effectiveNumerator = 4 - 1 = 3
     *   effectiveDenominator = 6 - 1 - 1 = 4
     *   score = 3 / 4 = 0.75
     * </pre>
     *
     * @param numerator Numerator population count
     * @param numeratorExclusion Numerator exclusion count
     * @param denominator Denominator population count
     * @param denominatorExclusion Denominator exclusion count
     * @param denominatorException Denominator exception count
     * @return The calculated score, or {@code null} if denominator is 0
     */
    public static Double calculateProportionScore(
            int numerator,
            int numeratorExclusion,
            int denominator,
            int denominatorExclusion,
            int denominatorException) {
        int effectiveNumerator = numerator - numeratorExclusion;
        int effectiveDenominator = denominator - denominatorExclusion - denominatorException;
        return calcProportionScore(effectiveNumerator, effectiveDenominator);
    }

    /**
     * Calculate ratio score for continuous variable measures.
     *
     * <p>Formula: {@code numeratorAggregate / denominatorAggregate}
     *
     * <p>Used for RATIO measures with MEASUREOBSERVATION populations where
     * numerator and denominator have separate observation aggregates.
     *
     * <p><strong>Edge Cases:</strong>
     * <ul>
     *   <li>If denominator is {@code null} or {@code 0.0}, returns {@code null}</li>
     *   <li>If numerator is {@code null} or {@code 0.0} and denominator &gt; 0, returns {@code 0.0}</li>
     *   <li>Otherwise, returns {@code numeratorAggregate / denominatorAggregate}</li>
     * </ul>
     *
     * @param numeratorAggregate Aggregated numerator observation value
     * @param denominatorAggregate Aggregated denominator observation value
     * @return The calculated score, or {@code null} if denominator is 0, or {@code 0.0} if numerator is 0
     */
    public static Double calculateRatioScore(double numeratorAggregate, double denominatorAggregate) {
        // Handle zero or null denominator
        if (denominatorAggregate == 0.0) {
            return null;
        }

        // Explicitly handle numerator zero with positive denominator
        if (numeratorAggregate == 0.0) {
            return denominatorAggregate > 0.0 ? 0.0 : null;
        }

        return numeratorAggregate / denominatorAggregate;
    }

    /**
     * Aggregate a list of QuantityDef values using the specified method.
     *
     * <p>Used for CONTINUOUSVARIABLE and RATIO continuous variable scoring.
     *
     * <p><strong>Aggregation Methods:</strong>
     * <ul>
     *   <li><strong>SUM</strong> - Sum all quantity values</li>
     *   <li><strong>AVG</strong> - Average all quantity values</li>
     *   <li><strong>MIN</strong> - Minimum quantity value</li>
     *   <li><strong>MAX</strong> - Maximum quantity value</li>
     *   <li><strong>MEDIAN</strong> - Median of all quantity values (even count: average of middle two)</li>
     *   <li><strong>COUNT</strong> - Count of quantity observations</li>
     * </ul>
     *
     * @param quantities List of quantity observations to aggregate
     * @param method Aggregation method (SUM, AVG, MIN, MAX, MEDIAN, COUNT)
     * @return Aggregated QuantityDef, or {@code null} if quantities is empty
     * @throws InvalidRequestException if method is N_A (NO-OP)
     * @throws IllegalArgumentException if method is unsupported
     */
    @Nullable
    public static QuantityDef aggregateContinuousVariable(
            List<QuantityDef> quantities, ContinuousVariableObservationAggregateMethod method) {
        if (quantities == null || quantities.isEmpty()) {
            return null;
        }

        if (ContinuousVariableObservationAggregateMethod.N_A == method) {
            throw new InvalidRequestException(
                    "Aggregate method must be provided for continuous variable scoring, but is NO-OP.");
        }

        // Enhanced switch with early returns - short-circuit logic
        return switch (method) {
            case COUNT -> new QuantityDef((double) quantities.size());
            case MEDIAN -> {
                List<Double> sorted = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();
                int n = sorted.size();
                double result = (n % 2 == 1) ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
                yield new QuantityDef(result);
            }
            case SUM -> {
                double result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .sum();
                yield new QuantityDef(result);
            }
            case MAX -> {
                double result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .max()
                        .orElse(Double.NaN);
                yield new QuantityDef(result);
            }
            case MIN -> {
                double result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .min()
                        .orElse(Double.NaN);
                yield new QuantityDef(result);
            }
            case AVG -> {
                double result = quantities.stream()
                        .map(QuantityDef::value)
                        .filter(Objects::nonNull)
                        .mapToDouble(value -> value)
                        .average()
                        .orElse(Double.NaN);
                yield new QuantityDef(result);
            }
            default -> throw new IllegalArgumentException("Unsupported aggregation method: " + method);
        };
    }

    /**
     * Collect QuantityDef objects from nested Map structures in resources.
     *
     * <p>Helper for continuous variable scoring. Extracts QuantityDef values from
     * resources that contain {@code Map<?, ?>} structures with QuantityDef values.
     *
     * <p><strong>Usage Pattern:</strong>
     * <pre>
     * // Collect quantities from population resources
     * List&lt;QuantityDef&gt; quantities = MeasureScoreCalculator.collectQuantities(resources);
     *
     * // Aggregate using specified method
     * QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
     *     quantities, ContinuousVariableObservationAggregateMethod.SUM);
     * </pre>
     *
     * @param resources Collection of objects that may contain Maps with QuantityDef values
     * @return List of QuantityDef objects found
     */
    public static List<QuantityDef> collectQuantities(Collection<Object> resources) {
        var mapValues = resources.stream()
                .filter(x -> x instanceof Map<?, ?>)
                .map(x -> (Map<?, ?>) x)
                .map(Map::values)
                .flatMap(Collection::stream)
                .toList();

        return mapValues.stream()
                .filter(QuantityDef.class::isInstance)
                .map(QuantityDef.class::cast)
                .toList();
    }

    /**
     * Private helper for proportion/ratio scoring: {@code numerator / denominator}
     *
     * <p>Handles null numerator as 0 and zero denominator as null score.
     *
     * @param numeratorCount Effective numerator count (after exclusions applied)
     * @param denominatorCount Effective denominator count (after exclusions/exceptions applied)
     * @return The calculated score, or {@code null} if denominator is 0
     */
    private static Double calcProportionScore(Integer numeratorCount, Integer denominatorCount) {
        if (numeratorCount == null) {
            numeratorCount = 0;
        }
        if (denominatorCount != null && denominatorCount != 0) {
            return numeratorCount / (double) denominatorCount;
        }
        return null;
    }
}
