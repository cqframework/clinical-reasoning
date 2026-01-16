package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class R4MeasureReportUtilsTest {

    @Test
    void testGetCountFromGroupPopulation_Found() {
        List<MeasureReportGroupPopulationComponent> populations =
                List.of(createGroupPopulation("numerator", 10), createGroupPopulation("denominator", 20));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(10, count);
    }

    @Test
    void testGetCountFromGroupPopulation_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = List.of(createGroupPopulation("numerator", 10));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOREXCLUSION);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromGroupPopulation_ByType() {
        List<MeasureReportGroupPopulationComponent> populations = List.of(createGroupPopulation("numerator", 15));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(15, count);
    }

    @Test
    void testGetCountFromGroupPopulation_FromGroupComponent() {
        MeasureReportGroupComponent group = createGroup(createGroupPopulation("denominator", 25));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                group, MeasurePopulationType.DENOMINATOR);

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_Found() {
        List<StratifierGroupPopulationComponent> populations =
                List.of(createStratifierPopulation("numerator", 5), createStratifierPopulation("denominator", 8));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(8, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_NotFound() {
        List<StratifierGroupPopulationComponent> populations = List.of(createStratifierPopulation("numerator", 5));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_ByType() {
        List<StratifierGroupPopulationComponent> populations = List.of(createStratifierPopulation("numerator", 7));

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(populations, MeasurePopulationType.NUMERATOR);

        assertEquals(7, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_FromStratum() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();
        stratum.addPopulation(createStratifierPopulation("denominator", 12));

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(stratum, MeasurePopulationType.DENOMINATOR);

        assertEquals(12, count);
    }

    @Test
    void testGetPopulationTypes() {
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulation("numerator", 0),
                createGroupPopulation("denominator", 0),
                createGroupPopulation("denominator-exclusion", 0));

        Set<MeasurePopulationType> types = R4MeasureReportUtils.getPopulationTypes(group);

        assertNotNull(types);
        assertEquals(3, types.size());
        assertTrue(types.contains(MeasurePopulationType.NUMERATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOREXCLUSION));
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_True() {
        MeasureReportGroupPopulationComponent numerator = createGroupPopulation("numerator", 0);

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.NUMERATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_False() {
        MeasureReportGroupPopulationComponent numerator = createGroupPopulation("numerator", 0);

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.DENOMINATOR);

        assertFalse(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_True() {
        StratifierGroupPopulationComponent denominator = createStratifierPopulation("denominator", 0);

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.DENOMINATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_False() {
        StratifierGroupPopulationComponent denominator = createStratifierPopulation("denominator", 0);

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.NUMERATOR);

        assertFalse(matches);
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationType - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationType_MultiplePopulationsWithSameType_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = List.of(
                createGroupPopulation("numerator", 10, "numerator-1"),
                createGroupPopulation("numerator", 20, "numerator-2"));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                        populations, MeasurePopulationType.NUMERATOR));

        assertTrue(exception.getMessage().contains("Expected only a single population"));
        assertTrue(exception.getMessage().contains("NUMERATOR"));
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationId - Happy Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationId_Found() {
        List<MeasureReportGroupPopulationComponent> populations = List.of(
                createGroupPopulation("numerator", 15, "numerator-1"),
                createGroupPopulation("denominator", 30, "denominator-1"));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "denominator-1");

        assertEquals(30, count);
    }

    @Test
    void testGetCountFromGroupPopulationByPopulationId_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations =
                List.of(createGroupPopulation("numerator", 15, "numerator-1"));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationId - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationId_MultiplePopulationsWithSameId_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = List.of(
                createGroupPopulation("numerator", 10, "duplicate-id"),
                createGroupPopulation("denominator", 20, "duplicate-id"));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "duplicate-id"));

        assertTrue(exception.getMessage().contains("Expected only a single population"));
        assertTrue(exception.getMessage().contains("duplicate-id"));
    }

    // ========================================
    // Tests for getCountFromGroupPopulationById (convenience method)
    // ========================================

    @Test
    void testGetCountFromGroupPopulationById_FromGroupComponent() {
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulation("numerator", 42, "numerator-1"),
                createGroupPopulation("denominator", 100, "denominator-1"));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "numerator-1");

        assertEquals(42, count);
    }

    @Test
    void testGetCountFromGroupPopulationById_NotFound() {
        MeasureReportGroupComponent group = createGroup(createGroupPopulation("numerator", 42, "numerator-1"));

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromStratumPopulationByType - Failure Path
    // ========================================

    @Test
    void testGetCountFromStratumPopulationByType_MultiplePopulationsWithSameType_ThrowsException() {
        List<StratifierGroupPopulationComponent> populations =
                List.of(createStratifierPopulation("numerator", 5), createStratifierPopulation("numerator", 7));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getCountFromStratumPopulationByType(
                        populations, MeasurePopulationType.NUMERATOR));

        assertTrue(exception.getMessage().contains("Got back more than one stratum population"));
        assertTrue(exception.getMessage().contains("NUMERATOR"));
    }

    // ========================================
    // Tests for getCountFromStratumPopulationById - Happy Path
    // ========================================

    @Test
    void testGetCountFromStratumPopulationById_FromList_Found() {
        List<StratifierGroupPopulationComponent> populations = List.of(
                createStratifierPopulation("numerator", 8, "num-1"),
                createStratifierPopulation("denominator", 16, "den-1"));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "den-1");

        assertEquals(16, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromList_NotFound() {
        List<StratifierGroupPopulationComponent> populations =
                List.of(createStratifierPopulation("numerator", 8, "num-1"));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "missing-id");

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_Found() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();
        stratum.addPopulation(createStratifierPopulation("numerator", 25, "num-1"));
        stratum.addPopulation(createStratifierPopulation("denominator", 50, "den-1"));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "num-1");

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_NotFound() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();
        stratum.addPopulation(createStratifierPopulation("numerator", 25, "num-1"));

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "missing-id");

        assertEquals(0, count);
    }

    // Note: testGetStratumDefText and testMatchesStratumValue tests omitted
    // These methods operate on complex internal data structures (StratumDef, StratifierDef, etc.)
    // that are tested through integration tests in the measure evaluation suite.
    // The utility methods themselves are extracted from proven working code in
    // R4MeasureReportScorer and R4MeasureReportBuilder.

    // ========================================
    // Tests for hasAnyPopulationOfType
    // ========================================

    @Test
    void testHasAnyPopulationOfType_Found() {
        MeasureReportGroupComponent group =
                createGroup(createGroupPopulation("numerator", 10), createGroupPopulation("denominator", 20));

        assertTrue(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.NUMERATOR));
        assertTrue(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.DENOMINATOR));
    }

    @Test
    void testHasAnyPopulationOfType_NotFound() {
        MeasureReportGroupComponent group = createGroup(createGroupPopulation("numerator", 10));

        assertFalse(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.DENOMINATOR));
        assertFalse(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.INITIALPOPULATION));
    }

    @Test
    void testHasAnyPopulationOfType_EmptyGroup() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        assertFalse(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.NUMERATOR));
    }

    @Test
    void testHasAnyPopulationOfType_MeasureObservation() {
        MeasureReportGroupComponent group = createGroup(createGroupPopulation("measure-observation", 5));

        assertTrue(R4MeasureReportUtils.hasAnyPopulationOfType(group, MeasurePopulationType.MEASUREOBSERVATION));
    }

    // ========================================
    // Tests for hasAggregationMethod (group variant)
    // ========================================

    @Test
    void testHasAggregationMethod_Group_WithMatchingMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group =
                createGroup(createGroupPopulationWithAggregation("measure-observation", 0, "sum"));

        assertTrue(R4MeasureReportUtils.hasAggregationMethod(
                measureUrl, group, ContinuousVariableObservationAggregateMethod.SUM));
    }

    @Test
    void testHasAggregationMethod_Group_WithNonMatchingMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group =
                createGroup(createGroupPopulationWithAggregation("measure-observation", 0, "avg"));

        assertFalse(R4MeasureReportUtils.hasAggregationMethod(
                measureUrl, group, ContinuousVariableObservationAggregateMethod.SUM));
    }

    @Test
    void testHasAggregationMethod_Group_WithNoMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(createGroupPopulation("numerator", 0));

        assertTrue(R4MeasureReportUtils.hasAggregationMethod(
                measureUrl, group, ContinuousVariableObservationAggregateMethod.N_A));
    }

    @Test
    void testHasAggregationMethod_Group_WithMultiplePopulationsAndOneWithMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulation("numerator", 0),
                createGroupPopulationWithAggregation("measure-observation", 0, "max"));

        assertTrue(R4MeasureReportUtils.hasAggregationMethod(
                measureUrl, group, ContinuousVariableObservationAggregateMethod.MAX));
    }

    // ========================================
    // Tests for getAggregationMethodFromGroup
    // ========================================

    @Test
    void testGetAggregationMethodFromGroup_WithSinglePopulationWithMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group =
                createGroup(createGroupPopulationWithAggregation("measure-observation", 0, "avg"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.AVG, result);
    }

    @Test
    void testGetAggregationMethodFromGroup_WithMultiplePopulationsAndOneWithMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulation("numerator", 0),
                createGroupPopulationWithAggregation("measure-observation", 0, "median"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.MEDIAN, result);
    }

    @Test
    void testGetAggregationMethodFromGroup_WithAllPopulationsHavingN_A() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group =
                createGroup(createGroupPopulation("numerator", 0), createGroupPopulation("denominator", 0));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregationMethodFromGroup_WithEmptyGroup() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregationMethodFromGroup_WithMultipleDifferentMethods_ThrowsException() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulationWithAggregation("measure-observation", 0, "sum"),
                createGroupPopulationWithAggregation("measure-observation", 0, "avg"));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group));

        assertTrue(exception.getMessage().contains("Expected only one aggregation method"));
    }

    @Test
    void testGetAggregationMethodFromGroup_WithMultiplePopulationsWithSameMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulationWithAggregation("measure-observation", 0, "min"),
                createGroupPopulationWithAggregation("measure-observation", 0, "min"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.MIN, result);
    }

    @Test
    void testGetAggregationMethodFromGroup_WithMixOfN_AAndRealMethod() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupComponent group = createGroup(
                createGroupPopulation("numerator", 0),
                createGroupPopulation("denominator", 0),
                createGroupPopulationWithAggregation("measure-observation", 0, "count"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromGroup(measureUrl, group);

        assertEquals(ContinuousVariableObservationAggregateMethod.COUNT, result);
    }

    // ========================================
    // Tests for getAggregateMethod
    // ========================================

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithSumExtension() {
        testAggregationMethod("sum", ContinuousVariableObservationAggregateMethod.SUM);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithAvgExtension() {
        testAggregationMethod("avg", ContinuousVariableObservationAggregateMethod.AVG);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithCountExtension() {
        testAggregationMethod("count", ContinuousVariableObservationAggregateMethod.COUNT);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithMinExtension() {
        testAggregationMethod("min", ContinuousVariableObservationAggregateMethod.MIN);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithMaxExtension() {
        testAggregationMethod("max", ContinuousVariableObservationAggregateMethod.MAX);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_WithMedianExtension() {
        testAggregationMethod("median", ContinuousVariableObservationAggregateMethod.MEDIAN);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_NoExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromPopulation(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregationMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.N_A));
        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_NullPopulation() {
        String measureUrl = "http://example.com/Measure/test";

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromPopulation(measureUrl, null);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregationMethod_FromPopulation_FromGroup_InvalidExtensionValue() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population =
                createGroupPopulationWithAggregation("measure-observation", 0, "invalid-method");

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureReportUtils.getAggregationMethodFromPopulation(measureUrl, population));

        assertTrue(exception.getMessage().contains("Aggregation method: invalid-method is not a valid value"));
        assertTrue(exception.getMessage().contains(measureUrl));
    }

    // ========================================
    // Tests for getAggregationResult
    // ========================================

    @Test
    void testGetAggregateResult_WithExtension() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(42.5));

        BigDecimal result = R4MeasureReportUtils.getAggregateResult(population);

        assertEquals(new BigDecimal("42.5"), result);
    }

    @Test
    void testGetAggregateResult_NoExtension() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        BigDecimal result = R4MeasureReportUtils.getAggregateResult(population);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testGetAggregateResult_WithWrongExtensionType() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        // Add extension with wrong type (StringType instead of DecimalType)
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new StringType("42.5"));

        BigDecimal result = R4MeasureReportUtils.getAggregateResult(population);

        // Should return ZERO because the extension value is not a DecimalType
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testGetAggregateResult_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(0.0));

        BigDecimal result = R4MeasureReportUtils.getAggregateResult(population);

        assertEquals(new BigDecimal("0.0"), result);
    }

    @Test
    void testGetAggregateResult_WithNegativeValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(-15.75));

        BigDecimal result = R4MeasureReportUtils.getAggregateResult(population);

        assertEquals(new BigDecimal("-15.75"), result);
    }

    // ========================================
    // Tests for addAggregationResultAndMethod - with Double
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_FromDouble_WithValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.AVG, 99.99);

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(new BigDecimal("99.99"), ((DecimalType) resultExt.getValue()).getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("avg", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromDouble_WithNullValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.SUM, (Double) null);

        // Assert neither extension is set when value is null
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when value is null");
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when value is null");
    }

    @Test
    void testAddAggregationResultAndMethod_FromDouble_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.COUNT, 0.0);

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(new BigDecimal("0.0"), ((DecimalType) resultExt.getValue()).getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("count", ((StringType) methodExt.getValue()).getValue());
    }

    // ========================================
    // Tests for addAggregationResultAndMethod - with BigDecimal
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.AVG, new BigDecimal("123.456"));

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(new BigDecimal("123.456"), ((DecimalType) resultExt.getValue()).getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("avg", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithNullValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.SUM, (BigDecimal) null);

        // Assert neither extension is set when value is null
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when value is null");
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when value is null");
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.COUNT, BigDecimal.ZERO);

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        // Use compareTo for BigDecimal comparison to ignore scale differences (0 vs 0.0)
        assertEquals(0, new BigDecimal("0").compareTo(((DecimalType) resultExt.getValue()).getValue()));

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("count", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithNullMethod() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(population, null, new BigDecimal("50.5"));

        // Assert neither extension is set when method is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is null");
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.N_A, new BigDecimal("75.25"));

        // Assert neither extension is set when method is N_A
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is N_A");
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithNegativeValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.MIN, new BigDecimal("-42.75"));

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(new BigDecimal("-42.75"), ((DecimalType) resultExt.getValue()).getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("min", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithHighPrecisionValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.MEDIAN, new BigDecimal("3.141592653589793"));

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        // Note: conversion to double may lose precision
        assertInstanceOf(DecimalType.class, resultExt.getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("median", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_UpdatesExistingExtensions() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        // Add initial values
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("sum"));
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(10.0));

        // Update with new values
        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.MAX, new BigDecimal("999.99"));

        // Assert extensions are updated, not duplicated
        assertEquals(2, population.getExtension().size(), "Should have exactly 2 extensions (not duplicated)");

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(new BigDecimal("999.99"), ((DecimalType) resultExt.getValue()).getValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("max", ((StringType) methodExt.getValue()).getValue());
    }

    // ========================================
    // Tests for addAggregationResultAndMethod - with PopulationDef
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithAggregationResult() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                "pop1",
                MeasurePopulationType.NUMERATOR,
                null,
                ContinuousVariableObservationAggregateMethod.SUM,
                BigDecimal.valueOf(42.5));

        R4MeasureReportUtils.addAggregationResultAndMethod(population, populationDef);

        // Assert both extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("sum", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(new BigDecimal("42.5"), ((DecimalType) resultExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithNullMethod() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef =
                createPopulationDef("pop1", MeasurePopulationType.NUMERATOR, null, null, BigDecimal.valueOf(10.0));

        R4MeasureReportUtils.addAggregationResultAndMethod(population, populationDef);

        // Assert neither extension is set when method is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when aggregateMethod is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when aggregateMethod is null");
    }

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                "pop1",
                MeasurePopulationType.NUMERATOR,
                null,
                ContinuousVariableObservationAggregateMethod.N_A,
                BigDecimal.valueOf(10.0));

        R4MeasureReportUtils.addAggregationResultAndMethod(population, populationDef);

        // Assert neither extension is set when method is N_A
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when aggregateMethod is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when aggregateMethod is N_A");
    }

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithNullResult() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                "pop1", MeasurePopulationType.NUMERATOR, null, ContinuousVariableObservationAggregateMethod.AVG, null);

        R4MeasureReportUtils.addAggregationResultAndMethod(population, populationDef);

        // Assert neither extension is set when result is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when aggregationResult is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when aggregationResult is null");
    }

    // ========================================
    // Tests for addAggregationResultAndMethod - with ContinuousVariableObservationAggregateMethod and Double
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithAllAggregationMethods() {
        // Test multiple aggregate methods in a single test to reduce duplication
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MIN, 1.2, "min");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MAX, 1.3, "max");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MEDIAN, 1.4, "median");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.SUM, 1.5, "sum");
    }

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithNullMethod() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, (ContinuousVariableObservationAggregateMethod) null, 1.6);

        // Assert neither extension is set when method is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is null");
    }

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.N_A, 1.7);

        // Assert neither extension is set when method is N_A
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is N_A");
    }

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithNullResult() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, ContinuousVariableObservationAggregateMethod.AVG, (Double) null);

        // Assert neither extension is set when result is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when result is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when result is null");
    }

    /**
     * Helper method to test aggregation method extraction from population.
     */
    private void testAggregationMethod(
            String methodString, ContinuousVariableObservationAggregateMethod expectedMethod) {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population =
                createGroupPopulationWithAggregation("measure-observation", 0, methodString);

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregationMethodFromPopulation(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregationMethod(measureUrl, population, expectedMethod));
        assertEquals(expectedMethod, result);
    }

    /**
     * Helper method to test aggregation method and result together.
     */
    private void testAggregationMethodAndResult(
            ContinuousVariableObservationAggregateMethod method, Double result, String expectedMethodText) {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultAndMethod(population, method, result);

        // Assert both extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt, "Method extension should be set for " + expectedMethodText);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals(expectedMethodText, ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt, "Result extension should be set for " + expectedMethodText);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(BigDecimal.valueOf(result), ((DecimalType) resultExt.getValue()).getValue());
    }

    // ========================================
    // Helper methods
    // ========================================

    /**
     * Helper to create a PopulationDef for testing with an aggregationResult.
     */
    private PopulationDef createPopulationDef(
            String id,
            MeasurePopulationType type,
            String criteriaReference,
            ContinuousVariableObservationAggregateMethod aggregateMethod,
            BigDecimal aggregationResult) {
        ConceptDef code = new ConceptDef(List.of(new CodeDef("system", type.toCode())), null);
        CodeDef populationBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef populationDef = new PopulationDef(
                id, code, type, "TestExpression", populationBasis, criteriaReference, aggregateMethod, List.of());

        // Set aggregation result if provided (convert BigDecimal to Double)
        if (aggregationResult != null) {
            populationDef.setAggregationResult(aggregationResult.doubleValue());
        }

        return populationDef;
    }

    /**
     * Create a CodeableConcept with a single Coding code.
     */
    private static CodeableConcept createCodeableConcept(String code) {
        return new CodeableConcept().addCoding(new Coding().setCode(code));
    }

    /**
     * Create a MeasureReportGroupPopulationComponent with code and count.
     */
    private static MeasureReportGroupPopulationComponent createGroupPopulation(String code, int count) {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.setCode(createCodeableConcept(code));
        population.setCount(count);
        return population;
    }

    /**
     * Create a MeasureReportGroupPopulationComponent with code, count, and ID.
     */
    private static MeasureReportGroupPopulationComponent createGroupPopulation(String code, int count, String id) {
        MeasureReportGroupPopulationComponent population = createGroupPopulation(code, count);
        population.setId(id);
        return population;
    }

    /**
     * Create a MeasureReportGroupPopulationComponent with code, count, and aggregation method.
     */
    private static MeasureReportGroupPopulationComponent createGroupPopulationWithAggregation(
            String code, int count, String aggregationMethod) {
        MeasureReportGroupPopulationComponent population = createGroupPopulation(code, count);
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType(aggregationMethod));
        return population;
    }

    /**
     * Create a StratifierGroupPopulationComponent with code and count.
     */
    private static StratifierGroupPopulationComponent createStratifierPopulation(String code, int count) {
        StratifierGroupPopulationComponent population = new StratifierGroupPopulationComponent();
        population.setCode(createCodeableConcept(code));
        population.setCount(count);
        return population;
    }

    /**
     * Create a StratifierGroupPopulationComponent with code, count, and ID.
     */
    private static StratifierGroupPopulationComponent createStratifierPopulation(String code, int count, String id) {
        StratifierGroupPopulationComponent population = createStratifierPopulation(code, count);
        population.setId(id);
        return population;
    }

    /**
     * Create a MeasureReportGroupComponent with the given populations.
     */
    private static MeasureReportGroupComponent createGroup(MeasureReportGroupPopulationComponent... populations) {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();
        for (MeasureReportGroupPopulationComponent population : populations) {
            group.addPopulation(population);
        }
        return group;
    }
}
