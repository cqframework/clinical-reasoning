package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;

import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ConceptDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasurePopulationType;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

class R4MeasureReportUtilsTest {

    // ========================================
    // Tests for addAggregationResultAndMethod - with Double
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromDouble_WithValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.AVG, 99.99, "");

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(99.99, ((DecimalType) resultExt.getValue()).getValue().doubleValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("avg", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromDouble_WithNullValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.SUM, null, "");

        // Assert neither extension is set when value is null
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when value is null");
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when value is null");
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromDouble_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.COUNT, 0.0, "");

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(
                0.0, ((DecimalType) resultExt.getValue()).getValueAsNumber().doubleValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("count", ((StringType) methodExt.getValue()).getValue());
    }

    // ========================================
    // Tests for addAggregationResultAndMethod - with BigDecimal
    // ========================================

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.AVG, 123.456, "");

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(123.456, ((DecimalType) resultExt.getValue()).getValue().doubleValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("avg", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithNullValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.SUM, null, "");

        // Assert neither extension is set when value is null
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when value is null");
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when value is null");
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.COUNT, 0.0, "");

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        // Use compareTo for BigDecimal comparison to ignore scale differences (0 vs 0.0)
        assertEquals(0, BigDecimal.ZERO.compareTo(((DecimalType) resultExt.getValue()).getValue()));

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("count", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromBigDecimal_WithNullMethodAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, null, 50.5, "");

        // Assert method and result extensions are not set when method is null, but criteriaReference is added
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is null");

        // criteriaReference should be added even when method is null
        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt, "Criteria reference extension should be added even when method is null");
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.N_A, 75.25, "");

        // Assert method and result extensions are not set when method is N_A, but criteriaReference is added
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is N_A");

        // criteriaReference should be added even when method is N_A
        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt, "Criteria reference extension should be added even when method is N_A");
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithNegativeValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.MIN, -42.75, "");

        // Assert both extensions are set
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(-42.75, ((DecimalType) resultExt.getValue()).getValue().doubleValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("min", ((StringType) methodExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_WithHighPrecisionValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.MEDIAN, 3.141592653589793, "");

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
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromBigDecimal_UpdatesExistingExtensions() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        // Add initial values
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("sum"));
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(10.0));

        // Update with new values (pass null for criteriaReference to test only method and result)
        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.MAX, 999.99, null);

        // Assert extensions are updated, not duplicated
        assertEquals(2, population.getExtension().size(), "Should have exactly 2 extensions (not duplicated)");

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(999.99, ((DecimalType) resultExt.getValue()).getValue().doubleValue());

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("max", ((StringType) methodExt.getValue()).getValue());
    }

    // ========================================
    // Tests for addAggregationResultAndMethodAndCriteriaReference - with PopulationDef
    // ========================================

    @Test
    void
            testaddAggregationResultAndMethodAndCriteriaReference_FromPopulationDef_WithAggregationResultAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                MeasurePopulationType.NUMERATOR,
                null,
                ContinuousVariableObservationAggregateMethod.SUM,
                BigDecimal.valueOf(42.5));

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

        // Assert both extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals("sum", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(
                42.5, ((DecimalType) resultExt.getValue()).getValueAsNumber().doubleValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithNullMethodAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef =
                createPopulationDef(MeasurePopulationType.NUMERATOR, null, null, BigDecimal.valueOf(10.0));

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

        // Assert neither extension is set when method is null
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when aggregateMethod is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when aggregateMethod is null");
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromPopulationDef_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                MeasurePopulationType.NUMERATOR,
                null,
                ContinuousVariableObservationAggregateMethod.N_A,
                BigDecimal.valueOf(10.0));

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

        // Assert neither extension is set when method is N_A
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when aggregateMethod is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when aggregateMethod is N_A");
    }

    @Test
    void testAddAggregationResultAndMethod_FromPopulationDef_WithNullResultAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                MeasurePopulationType.NUMERATOR, null, ContinuousVariableObservationAggregateMethod.AVG, null);

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

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
    void testAddAggregationResultAndMethod_FromEnum_WithAllAggregationMethodsAndCriteriaReference() {
        // Test multiple aggregate methods in a single test to reduce duplication
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MIN, 1.2, "min");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MAX, 1.3, "max");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.MEDIAN, 1.4, "median");
        testAggregationMethodAndResult(ContinuousVariableObservationAggregateMethod.SUM, 1.5, "sum");
    }

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithNullMethodAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, null, 1.6, "");

        // Assert method and result extensions are not set when method is null, but criteriaReference is added
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is null");

        // criteriaReference should be added even when method is null
        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt, "Criteria reference extension should be added even when method is null");
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_AndCriteriaReference_FromEnum_WithN_A() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.N_A, 1.7, "");

        // Assert method and result extensions are not set when method is N_A, but criteriaReference is added
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when method is N_A");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when method is N_A");

        // criteriaReference should be added even when method is N_A
        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt, "Criteria reference extension should be added even when method is N_A");
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultAndMethod_FromEnum_WithNullResultAndCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.AVG, null, "");

        // Assert method and result extensions are not set when result is null, but criteriaReference is added
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNull(methodExt, "No method extension should be added when result is null");
        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNull(resultExt, "No result extension should be added when result is null");

        // criteriaReference should be added even when result is null
        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt, "Criteria reference extension should be added even when result is null");
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    // ========================================
    // Tests for criteriaReference in addAggregationResultMethodAndCriteriaRef
    // ========================================

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_WithValidCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.SUM, 100.5, "Numerator");

        // Assert all three extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("sum", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(
                100.5, ((DecimalType) resultExt.getValue()).getValueAsNumber().doubleValue());

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("Numerator", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_WithNullCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.AVG, 50.25, null);

        // Assert method and result extensions are set, but not criteriaReference
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNull(criteriaRefExt, "No criteria reference extension should be added when criteriaReference is null");
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_WithEmptyStringCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.MAX, 75.0, "");

        // Assert method and result extensions are set, and criteriaReference is set to empty string
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_UpdatesExistingCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        // Add initial values
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("sum"));
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(10.0));
        population.addExtension(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE, new StringType("OldReference"));

        // Update with new values including criteriaReference
        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.AVG, 999.99, "NewReference");

        // Assert extensions are updated, not duplicated
        assertEquals(3, population.getExtension().size(), "Should have exactly 3 extensions (not duplicated)");

        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("avg", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(
                999.99, ((DecimalType) resultExt.getValue()).getValueAsNumber().doubleValue());

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("NewReference", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_WithDenominatorReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.MEDIAN, 42.0, "Denominator");

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("Denominator", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_FromBigDecimal_WithCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(
                population, ContinuousVariableObservationAggregateMethod.COUNT, 123.456, "Numerator");

        // Assert all three extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("count", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("Numerator", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_FromPopulationDef_WithCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                MeasurePopulationType.MEASUREOBSERVATION,
                "Numerator",
                ContinuousVariableObservationAggregateMethod.MIN,
                BigDecimal.valueOf(15.5));

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

        // Assert all three extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);
        assertEquals("min", ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);
        assertEquals(15.5, ((DecimalType) resultExt.getValue()).getValue().doubleValue());

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNotNull(criteriaRefExt);
        assertEquals("Numerator", ((StringType) criteriaRefExt.getValue()).getValue());
    }

    @Test
    void testAddAggregationResultMethodAndCriteriaRef_FromPopulationDef_WithNullCriteriaReference() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        PopulationDef populationDef = createPopulationDef(
                MeasurePopulationType.MEASUREOBSERVATION,
                null,
                ContinuousVariableObservationAggregateMethod.MAX,
                BigDecimal.valueOf(88.8));

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, populationDef);

        // Assert method and result extensions are set, but not criteriaReference
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt);

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt);

        Extension criteriaRefExt = population.getExtensionByUrl(MeasureConstants.EXT_CQFM_CRITERIA_REFERENCE);
        assertNull(
                criteriaRefExt,
                "No criteria reference extension should be added when PopulationDef has null criteriaReference");
    }

    /**
     * Helper method to test aggregation method and result together.
     */
    private void testAggregationMethodAndResult(
            ContinuousVariableObservationAggregateMethod method, Double result, String expectedMethodText) {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        R4MeasureReportUtils.addAggregationResultMethodAndCriteriaRef(population, method, result, "");

        // Assert both extensions are set
        Extension methodExt = population.getExtensionByUrl(EXT_CQFM_AGGREGATE_METHOD_URL);
        assertNotNull(methodExt, "Method extension should be set for " + expectedMethodText);
        assertInstanceOf(StringType.class, methodExt.getValue());
        assertEquals(expectedMethodText, ((StringType) methodExt.getValue()).getValue());

        Extension resultExt = population.getExtensionByUrl(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT);
        assertNotNull(resultExt, "Result extension should be set for " + expectedMethodText);
        assertInstanceOf(DecimalType.class, resultExt.getValue());
        assertEquals(
                result, ((DecimalType) resultExt.getValue()).getValueAsNumber().doubleValue());
    }

    // ========================================
    // Helper methods
    // ========================================

    /**
     * Helper to create a PopulationDef for testing with an aggregationResult.
     */
    private PopulationDef createPopulationDef(
            MeasurePopulationType type,
            String criteriaReference,
            ContinuousVariableObservationAggregateMethod aggregateMethod,
            BigDecimal aggregationResult) {
        ConceptDef code = new ConceptDef(List.of(new CodeDef("system", type.toCode())), null);
        CodeDef populationBasis = new CodeDef("http://hl7.org/fhir/fhir-types", "boolean");
        PopulationDef populationDef = new PopulationDef(
                "pop1", code, type, "TestExpression", populationBasis, criteriaReference, aggregateMethod, List.of());

        // Set aggregation result if provided (convert BigDecimal to Double)
        if (aggregationResult != null) {
            populationDef.setAggregationResult(aggregationResult.doubleValue());
        }

        return populationDef;
    }
}
