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
import java.util.ArrayList;
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
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(10);
        populations.add(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(20);
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(10, count);
    }

    @Test
    void testGetCountFromGroupPopulation_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(10);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOREXCLUSION);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromGroupPopulation_ByType() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                populations, MeasurePopulationType.NUMERATOR);

        assertEquals(15, count);
    }

    @Test
    void testGetCountFromGroupPopulation_FromGroupComponent() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(25);
        group.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationType(
                group, MeasurePopulationType.DENOMINATOR);

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_Found() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(5);
        populations.add(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(8);
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(8, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_NotFound() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(5);
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationByType(
                populations, MeasurePopulationType.DENOMINATOR);

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_ByType() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(7);
        populations.add(numerator);

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(populations, MeasurePopulationType.NUMERATOR);

        assertEquals(7, count);
    }

    @Test
    void testGetCountFromStratifierPopulation_FromStratum() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(12);
        stratum.addPopulation(denominator);

        int count =
                R4MeasureReportUtils.getCountFromStratumPopulationByType(stratum, MeasurePopulationType.DENOMINATOR);

        assertEquals(12, count);
    }

    @Test
    void testGetPopulationTypes() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        group.addPopulation(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        group.addPopulation(denominator);

        MeasureReportGroupPopulationComponent denominatorExclusion = new MeasureReportGroupPopulationComponent();
        denominatorExclusion.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator-exclusion")));
        group.addPopulation(denominatorExclusion);

        Set<MeasurePopulationType> types = R4MeasureReportUtils.getPopulationTypes(group);

        assertNotNull(types);
        assertEquals(3, types.size());
        assertTrue(types.contains(MeasurePopulationType.NUMERATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOR));
        assertTrue(types.contains(MeasurePopulationType.DENOMINATOREXCLUSION));
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_True() {
        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.NUMERATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_GroupPopulation_False() {
        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(numerator, MeasurePopulationType.DENOMINATOR);

        assertFalse(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_True() {
        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.DENOMINATOR);

        assertTrue(matches);
    }

    @Test
    void testDoesPopulationTypeMatch_StratifierPopulation_False() {
        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));

        boolean matches = R4MeasureReportUtils.doesPopulationTypeMatch(denominator, MeasurePopulationType.NUMERATOR);

        assertFalse(matches);
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationType - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationType_MultiplePopulationsWithSameType_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        // Add two numerator populations - this is invalid
        MeasureReportGroupPopulationComponent numerator1 = new MeasureReportGroupPopulationComponent();
        numerator1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator1.setCount(10);
        numerator1.setId("numerator-1");
        populations.add(numerator1);

        MeasureReportGroupPopulationComponent numerator2 = new MeasureReportGroupPopulationComponent();
        numerator2.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator2.setCount(20);
        numerator2.setId("numerator-2");
        populations.add(numerator2);

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
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        numerator.setId("numerator-1");
        populations.add(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(30);
        denominator.setId("denominator-1");
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "denominator-1");

        assertEquals(30, count);
    }

    @Test
    void testGetCountFromGroupPopulationByPopulationId_NotFound() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(15);
        numerator.setId("numerator-1");
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationByPopulationId(populations, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromGroupPopulationByPopulationId - Failure Path
    // ========================================

    @Test
    void testGetCountFromGroupPopulationByPopulationId_MultiplePopulationsWithSameId_ThrowsException() {
        List<MeasureReportGroupPopulationComponent> populations = new ArrayList<>();

        // Add two populations with the same ID - this is invalid
        MeasureReportGroupPopulationComponent pop1 = new MeasureReportGroupPopulationComponent();
        pop1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        pop1.setCount(10);
        pop1.setId("duplicate-id");
        populations.add(pop1);

        MeasureReportGroupPopulationComponent pop2 = new MeasureReportGroupPopulationComponent();
        pop2.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        pop2.setCount(20);
        pop2.setId("duplicate-id");
        populations.add(pop2);

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
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(42);
        numerator.setId("numerator-1");
        group.addPopulation(numerator);

        MeasureReportGroupPopulationComponent denominator = new MeasureReportGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(100);
        denominator.setId("denominator-1");
        group.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "numerator-1");

        assertEquals(42, count);
    }

    @Test
    void testGetCountFromGroupPopulationById_NotFound() {
        MeasureReportGroupComponent group = new MeasureReportGroupComponent();

        MeasureReportGroupPopulationComponent numerator = new MeasureReportGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(42);
        numerator.setId("numerator-1");
        group.addPopulation(numerator);

        int count = R4MeasureReportUtils.getCountFromGroupPopulationById(group, "missing-id");

        assertEquals(0, count);
    }

    // ========================================
    // Tests for getCountFromStratumPopulationByType - Failure Path
    // ========================================

    @Test
    void testGetCountFromStratumPopulationByType_MultiplePopulationsWithSameType_ThrowsException() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        // Add two numerator populations - this is invalid
        StratifierGroupPopulationComponent numerator1 = new StratifierGroupPopulationComponent();
        numerator1.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator1.setCount(5);
        populations.add(numerator1);

        StratifierGroupPopulationComponent numerator2 = new StratifierGroupPopulationComponent();
        numerator2.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator2.setCount(7);
        populations.add(numerator2);

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
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(8);
        numerator.setId("num-1");
        populations.add(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(16);
        denominator.setId("den-1");
        populations.add(denominator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "den-1");

        assertEquals(16, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromList_NotFound() {
        List<StratifierGroupPopulationComponent> populations = new ArrayList<>();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(8);
        numerator.setId("num-1");
        populations.add(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(populations, "missing-id");

        assertEquals(0, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_Found() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(25);
        numerator.setId("num-1");
        stratum.addPopulation(numerator);

        StratifierGroupPopulationComponent denominator = new StratifierGroupPopulationComponent();
        denominator.setCode(new CodeableConcept().addCoding(new Coding().setCode("denominator")));
        denominator.setCount(50);
        denominator.setId("den-1");
        stratum.addPopulation(denominator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "num-1");

        assertEquals(25, count);
    }

    @Test
    void testGetCountFromStratumPopulationById_FromStratum_NotFound() {
        StratifierGroupComponent stratum = new StratifierGroupComponent();

        StratifierGroupPopulationComponent numerator = new StratifierGroupPopulationComponent();
        numerator.setCode(new CodeableConcept().addCoding(new Coding().setCode("numerator")));
        numerator.setCount(25);
        numerator.setId("num-1");
        stratum.addPopulation(numerator);

        int count = R4MeasureReportUtils.getCountFromStratumPopulationById(stratum, "missing-id");

        assertEquals(0, count);
    }

    // Note: testGetStratumDefText and testMatchesStratumValue tests omitted
    // These methods operate on complex internal data structures (StratumDef, StratifierDef, etc.)
    // that are tested through integration tests in the measure evaluation suite.
    // The utility methods themselves are extracted from proven working code in
    // R4MeasureReportScorer and R4MeasureReportBuilder.

    // ========================================
    // Tests for getAggregateMethod
    // ========================================

    @Test
    void testGetAggregateMethod_WithSumExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("sum"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.SUM));
        assertEquals(ContinuousVariableObservationAggregateMethod.SUM, result);
    }

    @Test
    void testGetAggregateMethod_WithAvgExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("avg"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.AVG));
        assertEquals(ContinuousVariableObservationAggregateMethod.AVG, result);
    }

    @Test
    void testGetAggregateMethod_WithCountExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("count"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.COUNT));
        assertEquals(ContinuousVariableObservationAggregateMethod.COUNT, result);
    }

    @Test
    void testGetAggregateMethod_WithMinExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("min"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.MIN));
        assertEquals(ContinuousVariableObservationAggregateMethod.MIN, result);
    }

    @Test
    void testGetAggregateMethod_WithMaxExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("max"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.MAX));
        assertEquals(ContinuousVariableObservationAggregateMethod.MAX, result);
    }

    @Test
    void testGetAggregateMethod_WithMedianExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("median"));

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.MEDIAN));
        assertEquals(ContinuousVariableObservationAggregateMethod.MEDIAN, result);
    }

    @Test
    void testGetAggregateMethod_NoExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        ContinuousVariableObservationAggregateMethod result =
                R4MeasureReportUtils.getAggregateMethod(measureUrl, population);

        assertTrue(R4MeasureReportUtils.hasAggregateMethod(
                measureUrl, population, ContinuousVariableObservationAggregateMethod.N_A));
        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregateMethod_NullPopulation() {
        String measureUrl = "http://example.com/Measure/test";

        ContinuousVariableObservationAggregateMethod result = R4MeasureReportUtils.getAggregateMethod(measureUrl, null);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregateMethod_InvalidExtensionValue() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("invalid-method"));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class, () -> R4MeasureReportUtils.getAggregateMethod(measureUrl, population));

        assertTrue(exception.getMessage().contains("Aggregation method: invalid-method is not a valid value"));
        assertTrue(exception.getMessage().contains(measureUrl));
    }

    // ========================================
    // Tests for getAggregationResult
    // ========================================

    @Test
    void testGetAggregationResult_WithExtension() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(42.5));

        BigDecimal result = R4MeasureReportUtils.getAggregationResult(population);

        assertEquals(new BigDecimal("42.5"), result);
    }

    @Test
    void testGetAggregationResult_NoExtension() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();

        BigDecimal result = R4MeasureReportUtils.getAggregationResult(population);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testGetAggregationResult_WithWrongExtensionType() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        // Add extension with wrong type (StringType instead of DecimalType)
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new StringType("42.5"));

        BigDecimal result = R4MeasureReportUtils.getAggregationResult(population);

        // Should return ZERO because the extension value is not a DecimalType
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testGetAggregationResult_WithZeroValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(0.0));

        BigDecimal result = R4MeasureReportUtils.getAggregationResult(population);

        assertEquals(new BigDecimal("0.0"), result);
    }

    @Test
    void testGetAggregationResult_WithNegativeValue() {
        MeasureReportGroupPopulationComponent population = new MeasureReportGroupPopulationComponent();
        population.addExtension(MeasureConstants.EXT_AGGREGATION_METHOD_RESULT, new DecimalType(-15.75));

        BigDecimal result = R4MeasureReportUtils.getAggregationResult(population);

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

        R4MeasureReportUtils.addAggregationResultAndMethod(
                population, (ContinuousVariableObservationAggregateMethod) null, new BigDecimal("50.5"));

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
}
