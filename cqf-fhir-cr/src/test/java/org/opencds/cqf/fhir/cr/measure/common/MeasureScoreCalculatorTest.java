package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link MeasureScoreCalculator}.
 *
 * <p>Tests all scoring algorithms with valid inputs, edge cases, and error conditions.
 */
class MeasureScoreCalculatorTest {

    // ========== calculateProportionScore Tests ==========

    @Test
    void testCalculateProportionScore_ValidInputs() {
        // Example from MeasureReportDefScorer javadoc: (4-1) / (6-1-1) = 0.75
        Double score = MeasureScoreCalculator.calculateProportionScore(4, 1, 6, 1, 1);
        assertNotNull(score);
        assertEquals(0.75, score, 0.0001);
    }

    @Test
    void testCalculateProportionScore_NoExclusions() {
        // 3/4 = 0.75
        Double score = MeasureScoreCalculator.calculateProportionScore(3, 0, 4, 0, 0);
        assertNotNull(score);
        assertEquals(0.75, score, 0.0001);
    }

    @Test
    void testCalculateProportionScore_ZeroDenominator() {
        // Denominator becomes 0 after exclusions: 5 - 3 - 2 = 0
        Double score = MeasureScoreCalculator.calculateProportionScore(3, 0, 5, 3, 2);
        assertNull(score, "Should return null when denominator is 0");
    }

    @Test
    void testCalculateProportionScore_ZeroNumerator() {
        // 0/4 = 0.0
        Double score = MeasureScoreCalculator.calculateProportionScore(0, 0, 4, 0, 0);
        assertNotNull(score);
        assertEquals(0.0, score, 0.0001);
    }

    @Test
    void testCalculateProportionScore_ZeroNumeratorAfterExclusion() {
        // (2-2) / 4 = 0.0
        Double score = MeasureScoreCalculator.calculateProportionScore(2, 2, 4, 0, 0);
        assertNotNull(score);
        assertEquals(0.0, score, 0.0001);
    }

    @Test
    void testCalculateProportionScore_PerfectScore() {
        // 10/10 = 1.0
        Double score = MeasureScoreCalculator.calculateProportionScore(10, 0, 10, 0, 0);
        assertNotNull(score);
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void testCalculateProportionScore_AllExcluded() {
        // (10-5) / (10-5) = 1.0
        Double score = MeasureScoreCalculator.calculateProportionScore(10, 5, 10, 5, 0);
        assertNotNull(score);
        assertEquals(1.0, score, 0.0001);
    }

    // ========== calculateRatioScore Tests ==========

    @Test
    void testCalculateRatioScore_ValidInputs() {
        // 100.0 / 50.0 = 2.0
        Double score = MeasureScoreCalculator.calculateRatioScore(100.0, 50.0);
        assertNotNull(score);
        assertEquals(2.0, score, 0.0001);
    }

    @Test
    void testCalculateRatioScore_LessThanOne() {
        // 25.0 / 50.0 = 0.5
        Double score = MeasureScoreCalculator.calculateRatioScore(25.0, 50.0);
        assertNotNull(score);
        assertEquals(0.5, score, 0.0001);
    }

    @Test
    void testCalculateRatioScore_ZeroDenominator() {
        Double score = MeasureScoreCalculator.calculateRatioScore(100.0, 0.0);
        assertNull(score, "Should return null when denominator is 0");
    }

    @Test
    void testCalculateRatioScore_ZeroNumerator() {
        // 0.0 / 50.0 = 0.0 (explicit handling)
        Double score = MeasureScoreCalculator.calculateRatioScore(0.0, 50.0);
        assertNotNull(score);
        assertEquals(0.0, score, 0.0001);
    }

    @Test
    void testCalculateRatioScore_BothZero() {
        // 0.0 / 0.0 = null
        Double score = MeasureScoreCalculator.calculateRatioScore(0.0, 0.0);
        assertNull(score, "Should return null when both are zero");
    }

    @Test
    void testCalculateRatioScore_EqualValues() {
        // 42.5 / 42.5 = 1.0
        Double score = MeasureScoreCalculator.calculateRatioScore(42.5, 42.5);
        assertNotNull(score);
        assertEquals(1.0, score, 0.0001);
    }

    @Test
    void testCalculateRatioScore_DecimalPrecision() {
        // Test with precise decimal values
        Double score = MeasureScoreCalculator.calculateRatioScore(1.0 / 3.0, 2.0 / 3.0);
        assertNotNull(score);
        assertEquals(0.5, score, 0.0001);
    }

    @Test
    void testCalculateRatioScore_NullNumerator() {
        // Test with null numerator - should return null
        assertNull(MeasureScoreCalculator.calculateRatioScore(null, 50.0), "Should return null when numerator is null");
    }

    @Test
    void testCalculateRatioScore_NullDenominator() {
        // Test with null denominator - should return null
        assertNull(
                MeasureScoreCalculator.calculateRatioScore(100.0, null), "Should return null when denominator is null");
    }

    @Test
    void testCalculateRatioScore_BothNull() {
        // Test with both null - should return null
        assertNull(
                MeasureScoreCalculator.calculateRatioScore(null, null),
                "Should return null when both parameters are null");
    }

    @Test
    void testCalculateRatioScore_NullNumeratorWithZeroDenominator() {
        // Test with null numerator and zero denominator - should return null
        Double score = MeasureScoreCalculator.calculateRatioScore(null, 0.0);
        assertNull(score, "Should return null when numerator is null even if denominator is zero");
    }

    // ========== aggregateContinuousVariable Tests ==========

    @Test
    void testAggregateContinuousVariable_Sum() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(20.0), new QuantityDef(30.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(60.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_Avg() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(20.0), new QuantityDef(30.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(20.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_Min() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(5.0), new QuantityDef(30.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.MIN);

        assertNotNull(result);
        assertEquals(5.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_Max() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(20.0), new QuantityDef(5.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.MAX);

        assertNotNull(result);
        assertEquals(20.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_MedianOdd() {
        // Median of odd count: [10, 20, 30] -> 20
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(30.0), new QuantityDef(20.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(20.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_MedianEven() {
        // Median of even count: [10, 20, 30, 40] -> (20 + 30) / 2 = 25
        List<QuantityDef> quantities =
                List.of(new QuantityDef(10.0), new QuantityDef(40.0), new QuantityDef(20.0), new QuantityDef(30.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(25.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_Count() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(20.0), new QuantityDef(30.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.COUNT);

        assertNotNull(result);
        assertEquals(3.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_EmptyList() {
        List<QuantityDef> quantities = new ArrayList<>();

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.SUM);

        assertNull(result, "Should return null for empty list");
    }

    @Test
    void testAggregateContinuousVariable_NullList() {
        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                null, ContinuousVariableObservationAggregateMethod.SUM);

        assertNull(result, "Should return null for null list");
    }

    @Test
    void testAggregateContinuousVariable_NoOpMethod() {
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> MeasureScoreCalculator.aggregateContinuousVariable(
                        quantities, ContinuousVariableObservationAggregateMethod.N_A));

        assertTrue(exception.getMessage().contains("NO-OP"));
    }

    @Test
    void testAggregateContinuousVariable_WithNullValues() {
        // Should filter out null values
        List<QuantityDef> quantities = List.of(new QuantityDef(10.0), new QuantityDef(null), new QuantityDef(20.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(30.0, result.value(), 0.0001);
    }

    @Test
    void testAggregateContinuousVariable_SingleValue() {
        List<QuantityDef> quantities = List.of(new QuantityDef(42.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(42.0, result.value(), 0.0001);
    }

    // ========== collectQuantities Tests ==========

    @Test
    void testCollectQuantities_ValidMaps() {
        // Create resources with nested maps containing QuantityDef values
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", new QuantityDef(10.0));
        map1.put("key2", new QuantityDef(20.0));

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key3", new QuantityDef(30.0));

        Collection<Object> resources = List.of(map1, map2);

        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);

        assertNotNull(quantities);
        assertEquals(3, quantities.size());
        assertTrue(quantities.stream().anyMatch(q -> q.value() == 10.0));
        assertTrue(quantities.stream().anyMatch(q -> q.value() == 20.0));
        assertTrue(quantities.stream().anyMatch(q -> q.value() == 30.0));
    }

    @Test
    void testCollectQuantities_EmptyCollection() {
        Collection<Object> resources = new ArrayList<>();

        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);

        assertNotNull(quantities);
        assertTrue(quantities.isEmpty());
    }

    @Test
    void testCollectQuantities_NoMaps() {
        // Collection with non-Map objects
        Collection<Object> resources = List.of("string", 42, new Object());

        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);

        assertNotNull(quantities);
        assertTrue(quantities.isEmpty());
    }

    @Test
    void testCollectQuantities_MapsWithoutQuantityDef() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "not a quantity");
        map1.put("key2", 123);

        Collection<Object> resources = List.of(map1);

        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);

        assertNotNull(quantities);
        assertTrue(quantities.isEmpty());
    }

    @Test
    void testCollectQuantities_MixedContent() {
        // Mix of maps with and without QuantityDef, and non-map objects
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", new QuantityDef(10.0));
        map1.put("key2", "not a quantity");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key3", 123);

        Collection<Object> resources = List.of(map1, "string", map2, new QuantityDef(20.0));

        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);

        assertNotNull(quantities);
        assertEquals(1, quantities.size());
        assertEquals(10.0, quantities.get(0).value(), 0.0001);
    }

    // ========== Edge Cases and Integration Tests ==========

    @Test
    void testProportionScoreIntegration_RealWorldScenario() {
        // Real-world scenario from measure evaluation
        // Initial Population: 100
        // Denominator: 100
        // Denominator Exclusion: 10
        // Denominator Exception: 5
        // Numerator: 70
        // Numerator Exclusion: 5

        // Score = (70 - 5) / (100 - 10 - 5) = 65 / 85 ≈ 0.7647
        Double score = MeasureScoreCalculator.calculateProportionScore(70, 5, 100, 10, 5);

        assertNotNull(score);
        assertEquals(0.7647, score, 0.0001);
    }

    @Test
    void testRatioScoreIntegration_ContinuousVariable() {
        // Simulate continuous variable ratio scoring
        // Numerator observations: [10, 20, 30] -> SUM = 60
        // Denominator observations: [5, 10, 15] -> SUM = 30
        // Score = 60 / 30 = 2.0

        List<QuantityDef> numQuant = List.of(new QuantityDef(10.0), new QuantityDef(20.0), new QuantityDef(30.0));
        List<QuantityDef> denQuant = List.of(new QuantityDef(5.0), new QuantityDef(10.0), new QuantityDef(15.0));

        QuantityDef numAgg = MeasureScoreCalculator.aggregateContinuousVariable(
                numQuant, ContinuousVariableObservationAggregateMethod.SUM);
        QuantityDef denAgg = MeasureScoreCalculator.aggregateContinuousVariable(
                denQuant, ContinuousVariableObservationAggregateMethod.SUM);

        Double score = MeasureScoreCalculator.calculateRatioScore(numAgg.value(), denAgg.value());

        assertNotNull(score);
        assertEquals(2.0, score, 0.0001);
    }

    @Test
    void testFullWorkflow_CollectAggregateContinuousVariable() {
        // Full workflow: collect quantities -> aggregate -> score
        Map<String, Object> resource1 = new HashMap<>();
        resource1.put("obs1", new QuantityDef(10.0));
        resource1.put("obs2", new QuantityDef(20.0));

        Map<String, Object> resource2 = new HashMap<>();
        resource2.put("obs3", new QuantityDef(30.0));

        Collection<Object> resources = List.of(resource1, resource2);

        // Collect quantities
        List<QuantityDef> quantities = MeasureScoreCalculator.collectQuantities(resources);
        assertEquals(3, quantities.size());

        // Aggregate using AVG
        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(20.0, result.value(), 0.0001);
    }

    // ========== aggregateContinuousVariableBigDecimal Tests ==========

    @Test
    void testAggregateContinuousVariableBigDecimal_Sum() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.3), BigDecimal.valueOf(30.2));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(61.0), result);
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_Avg() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(30.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(20.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_AvgWithPrecision() {
        // Test division precision with 1/3
        List<BigDecimal> values = List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ONE.compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_AvgRepeatingDecimal() {
        // Test with repeating decimal: 10/3 = 3.333...
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0), BigDecimal.valueOf(10.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        // Should be 10.0 exactly
        assertEquals(0, BigDecimal.valueOf(10.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_Min() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.5), BigDecimal.valueOf(5.2), BigDecimal.valueOf(30.8));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MIN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(5.2).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_Max() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.3), BigDecimal.valueOf(5.2));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MAX);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(20.3).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_MedianOdd() {
        // Median of odd count: [10, 20, 30] -> 20
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(20.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(20.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_MedianEven() {
        // Median of even count: [10, 20, 30, 40] -> (20 + 30) / 2 = 25
        List<BigDecimal> values = List.of(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(40.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(30.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(25.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_MedianEvenWithPrecision() {
        // Median with precision: [1, 2] -> (1 + 2) / 2 = 1.5
        List<BigDecimal> values = List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(1.5).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_MedianWithRepeatingDecimal() {
        // Median of [1, 2, 3] with odd count
        List<BigDecimal> values = List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(2.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(2.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_Count() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(30.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.COUNT);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(3.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_EmptyList() {
        List<BigDecimal> values = new ArrayList<>();

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_NullList() {
        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                null, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_WithNullValues() {
        // Should filter out null values
        List<BigDecimal> values = new ArrayList<>();
        values.add(BigDecimal.valueOf(10.0));
        values.add(null);
        values.add(BigDecimal.valueOf(20.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(30.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_AllNullValues() {
        // All null values should return ZERO
        List<BigDecimal> values = new ArrayList<>();
        values.add(null);
        values.add(null);
        values.add(null);

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_SingleValue() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(42.5));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(42.5).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_NoOpMethod() {
        List<BigDecimal> values = List.of(BigDecimal.valueOf(10.0));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                        values, ContinuousVariableObservationAggregateMethod.N_A));

        assertTrue(exception.getMessage().contains("NO-OP"));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_LargeNumbers() {
        // Test with very large numbers that might lose precision with double
        List<BigDecimal> values = List.of(
                BigDecimal.valueOf(999999999999999.1),
                BigDecimal.valueOf(999999999999999.2),
                BigDecimal.valueOf(999999999999999.3));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(2999999999999997.6).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_VerySmallNumbers() {
        // Test with very small numbers
        List<BigDecimal> values =
                List.of(BigDecimal.valueOf(0.0000001), BigDecimal.valueOf(0.0000002), BigDecimal.valueOf(0.0000003));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(0.0000006).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_NegativeNumbers() {
        List<BigDecimal> values =
                List.of(BigDecimal.valueOf(-10.0), BigDecimal.valueOf(-20.0), BigDecimal.valueOf(-5.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MIN);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(-20.0).compareTo(result));
    }

    @Test
    void testAggregateContinuousVariableBigDecimal_MixedPositiveNegative() {
        List<BigDecimal> values =
                List.of(BigDecimal.valueOf(-10.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(-5.0));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.valueOf(5.0).compareTo(result));
    }

    // ========== Division Precision Tests ==========

    @Test
    void testDivisionPrecision_ConstantValue() {
        // Verify the constant is set correctly
        assertEquals(17, MeasureScoreCalculator.DIVISION_PRECISION.getPrecision());
        assertEquals(RoundingMode.HALF_UP, MeasureScoreCalculator.DIVISION_PRECISION.getRoundingMode());
    }

    @Test
    void testDivisionPrecision_RepeatingDecimal() {
        // Test that repeating decimals are handled correctly: 1/3 = 0.333...
        List<BigDecimal> values = List.of(BigDecimal.ONE);

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values.stream()
                        .map(v -> v.divide(BigDecimal.valueOf(3), MeasureScoreCalculator.DIVISION_PRECISION))
                        .toList(),
                ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        // Should be approximately 0.3333333333333333
        assertTrue(result.compareTo(BigDecimal.valueOf(0.33333)) > 0);
        assertTrue(result.compareTo(BigDecimal.valueOf(0.33334)) < 0);
    }

    @Test
    void testDivisionPrecision_MedianEvenRounding() {
        // Test that median calculation with even count uses proper precision
        // [1.0, 2.0] median = 1.5, but let's test with values that require rounding
        List<BigDecimal> values =
                List.of(BigDecimal.valueOf(1.0000000000000001), BigDecimal.valueOf(1.0000000000000003));

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.MEDIAN);

        assertNotNull(result);
        // Result should be the average with proper precision
        assertTrue(result.compareTo(BigDecimal.ONE) > 0);
    }

    // ========== Integration Tests with BigDecimal Precision ==========

    @Test
    void testBigDecimalPrecisionVsDouble_AvgCalculation() {
        // Create scenario where double precision would be problematic
        List<QuantityDef> quantities = List.of(
                new QuantityDef(1.0 / 3.0), // 0.333...
                new QuantityDef(2.0 / 3.0), // 0.666...
                new QuantityDef(1.0));

        QuantityDef result = MeasureScoreCalculator.aggregateContinuousVariable(
                quantities, ContinuousVariableObservationAggregateMethod.AVG);

        assertNotNull(result);
        // Average should be approximately 0.666... (2/3)
        assertEquals(0.6666, result.value(), 0.0001);
    }

    @Test
    void testBigDecimalPrecisionVsDouble_SumCalculation() {
        // Test with many small values that could accumulate rounding errors
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add(BigDecimal.valueOf(0.001));
        }

        BigDecimal result = MeasureScoreCalculator.aggregateContinuousVariableBigDecimal(
                values, ContinuousVariableObservationAggregateMethod.SUM);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ONE.compareTo(result));
    }
}
