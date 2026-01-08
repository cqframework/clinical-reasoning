package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.CQFM_SCORING_EXT_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_CQFM_AGGREGATE_METHOD_URL;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_DECREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.IMPROVEMENT_NOTATION_SYSTEM_INCREASE;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.CodeDef;
import org.opencds.cqf.fhir.cr.measure.common.ContinuousVariableObservationAggregateMethod;
import org.opencds.cqf.fhir.cr.measure.common.MeasureScoring;

class R4MeasureUtilsTest {

    @Test
    void testGetMeasureScoring_FromMeasure() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");
        measure.setScoring(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                        .setCode("proportion")));

        MeasureScoring scoring = R4MeasureUtils.getMeasureScoring(measure);

        assertNotNull(scoring);
        assertEquals(MeasureScoring.PROPORTION, scoring);
    }

    @Test
    void testGetMeasureScoring_FromCode_Valid() {
        String measureUrl = "http://example.com/Measure/test";
        String scoringCode = "ratio";

        MeasureScoring scoring = R4MeasureUtils.getMeasureScoring(measureUrl, scoringCode);

        assertNotNull(scoring);
        assertEquals(MeasureScoring.RATIO, scoring);
    }

    @Test
    void testGetMeasureScoring_FromCode_Null() {
        String measureUrl = "http://example.com/Measure/test";

        MeasureScoring scoring = R4MeasureUtils.getMeasureScoring(measureUrl, null);

        assertNull(scoring);
    }

    @Test
    void testGetMeasureScoring_FromCode_Invalid() {
        String measureUrl = "http://example.com/Measure/test";
        String scoringCode = "invalid-code";

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class, () -> R4MeasureUtils.getMeasureScoring(measureUrl, scoringCode));

        assertTrue(exception.getMessage().contains("not a valid Measure Scoring Type"));
    }

    @Test
    void testGetGroupMeasureScoring_WithExtension() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");

        MeasureGroupComponent group = new MeasureGroupComponent();
        group.addExtension(
                CQFM_SCORING_EXT_URL,
                new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                                .setCode("cohort")));

        MeasureScoring scoring = R4MeasureUtils.getGroupMeasureScoring(measure, group);

        assertNotNull(scoring);
        assertEquals(MeasureScoring.COHORT, scoring);
    }

    @Test
    void testGetGroupMeasureScoring_NoExtension() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");

        MeasureGroupComponent group = new MeasureGroupComponent();

        MeasureScoring scoring = R4MeasureUtils.getGroupMeasureScoring(measure, group);

        assertNull(scoring);
    }

    @Test
    void testGetGroupMeasureScoring_InvalidCode() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");

        MeasureGroupComponent group = new MeasureGroupComponent();
        group.addExtension(
                CQFM_SCORING_EXT_URL,
                new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                                .setCode("attestation")));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class, () -> R4MeasureUtils.getGroupMeasureScoring(measure, group));

        assertTrue(exception.getMessage().contains("attestation"));
        assertTrue(exception.getMessage().contains("not a valid Measure Scoring Type"));
    }

    @Test
    void testGetGroupImprovementNotation_WithExtension() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");

        MeasureGroupComponent group = new MeasureGroupComponent();
        group.addExtension(
                MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION,
                new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM)
                                .setCode(IMPROVEMENT_NOTATION_SYSTEM_INCREASE)));

        CodeDef codeDef = R4MeasureUtils.getGroupImprovementNotation(measure, group);

        assertNotNull(codeDef);
        assertEquals(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, codeDef.system());
        assertEquals(IMPROVEMENT_NOTATION_SYSTEM_INCREASE, codeDef.code());
    }

    @Test
    void testGetGroupImprovementNotation_NoExtension() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");

        MeasureGroupComponent group = new MeasureGroupComponent();

        CodeDef codeDef = R4MeasureUtils.getGroupImprovementNotation(measure, group);

        assertNull(codeDef);
    }

    @Test
    void testIsIncreaseImprovementNotation_Increase() {
        CodeDef codeDef = new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);

        boolean result = R4MeasureUtils.isIncreaseImprovementNotation(codeDef);

        assertTrue(result);
    }

    @Test
    void testIsIncreaseImprovementNotation_Decrease() {
        CodeDef codeDef = new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_DECREASE);

        boolean result = R4MeasureUtils.isIncreaseImprovementNotation(codeDef);

        assertFalse(result);
    }

    @Test
    void testIsIncreaseImprovementNotation_Null() {
        boolean result = R4MeasureUtils.isIncreaseImprovementNotation(null);

        assertTrue(result); // Default to true if null
    }

    @Test
    void testValidateImprovementNotationCode_Valid_Increase() {
        String measureUrl = "http://example.com/Measure/test";
        CodeDef codeDef = new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_INCREASE);

        // Should not throw exception
        R4MeasureUtils.validateImprovementNotationCode(measureUrl, codeDef);
    }

    @Test
    void testValidateImprovementNotationCode_Valid_Decrease() {
        String measureUrl = "http://example.com/Measure/test";
        CodeDef codeDef = new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, IMPROVEMENT_NOTATION_SYSTEM_DECREASE);

        // Should not throw exception
        R4MeasureUtils.validateImprovementNotationCode(measureUrl, codeDef);
    }

    @Test
    void testValidateImprovementNotationCode_InvalidCode() {
        String measureUrl = "http://example.com/Measure/test";
        CodeDef codeDef = new CodeDef(MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM, "invalid-code");

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureUtils.validateImprovementNotationCode(measureUrl, codeDef));

        assertTrue(exception.getMessage().contains("invalid System"));
    }

    @Test
    void testValidateImprovementNotationCode_InvalidSystem() {
        String measureUrl = "http://example.com/Measure/test";
        CodeDef codeDef = new CodeDef("http://invalid-system.com", IMPROVEMENT_NOTATION_SYSTEM_INCREASE);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> R4MeasureUtils.validateImprovementNotationCode(measureUrl, codeDef));

        assertTrue(exception.getMessage().contains("invalid System"));
    }

    @Test
    void testGetMeasureGroup_ById() {
        Measure measure = new Measure();

        MeasureGroupComponent group1 = new MeasureGroupComponent();
        group1.setId("group-1");
        measure.addGroup(group1);

        MeasureGroupComponent group2 = new MeasureGroupComponent();
        group2.setId("group-2");
        measure.addGroup(group2);

        MeasureGroupComponent result = R4MeasureUtils.getMeasureGroup(measure, "group-2");

        assertNotNull(result);
        assertEquals("group-2", result.getId());
    }

    @Test
    void testGetMeasureGroup_ById_NotFound() {
        Measure measure = new Measure();

        MeasureGroupComponent group1 = new MeasureGroupComponent();
        group1.setId("group-1");
        measure.addGroup(group1);

        MeasureGroupComponent result = R4MeasureUtils.getMeasureGroup(measure, "group-999");

        assertNull(result);
    }

    @Test
    void testGetMeasureGroup_ById_NullGroupId() {
        Measure measure = new Measure();

        MeasureGroupComponent result = R4MeasureUtils.getMeasureGroup(measure, (String) null);

        assertNull(result);
    }

    @Test
    void testGetMeasureGroup_ByReportGroup() {
        Measure measure = new Measure();

        MeasureGroupComponent group1 = new MeasureGroupComponent();
        group1.setId("group-1");
        measure.addGroup(group1);

        MeasureReportGroupComponent reportGroup = new MeasureReportGroupComponent();
        reportGroup.setId("group-1");

        MeasureGroupComponent result = R4MeasureUtils.getMeasureGroup(measure, reportGroup);

        assertNotNull(result);
        assertEquals("group-1", result.getId());
    }

    @Test
    void testGetMeasureGroup_ByReportGroup_NullReportGroupId() {
        Measure measure = new Measure();

        MeasureGroupComponent group1 = new MeasureGroupComponent();
        group1.setId("group-1");
        measure.addGroup(group1);

        MeasureReportGroupComponent reportGroup = new MeasureReportGroupComponent();

        MeasureGroupComponent result = R4MeasureUtils.getMeasureGroup(measure, reportGroup);

        assertNull(result);
    }

    // ========================================
    // Tests for computeScoring(String, MeasureScoring, MeasureScoring)
    // ========================================

    @Test
    void testComputeScoring_GroupScoringPresent_ReturnsGroupScoring() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureScoring measureScoring = MeasureScoring.PROPORTION;
        MeasureScoring groupScoring = MeasureScoring.RATIO;

        MeasureScoring result = R4MeasureUtils.computeScoring(measureUrl, measureScoring, groupScoring);

        assertEquals(MeasureScoring.RATIO, result);
    }

    @Test
    void testComputeScoring_OnlyMeasureScoringPresent_ReturnsMeasureScoring() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureScoring measureScoring = MeasureScoring.COHORT;
        MeasureScoring groupScoring = null;

        MeasureScoring result = R4MeasureUtils.computeScoring(measureUrl, measureScoring, groupScoring);

        assertEquals(MeasureScoring.COHORT, result);
    }

    @Test
    void testComputeScoring_BothNull_ThrowsException() {
        String measureUrl = "http://example.com/Measure/test";

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class, () -> R4MeasureUtils.computeScoring(measureUrl, null, null));

        assertTrue(exception.getMessage().contains("MeasureScoring must be specified"));
        assertTrue(exception.getMessage().contains(measureUrl));
    }

    @Test
    void testComputeScoring_GroupScoringOverridesMeasure() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureScoring measureScoring = MeasureScoring.PROPORTION;
        MeasureScoring groupScoring = MeasureScoring.CONTINUOUSVARIABLE;

        MeasureScoring result = R4MeasureUtils.computeScoring(measureUrl, measureScoring, groupScoring);

        // Group scoring should take precedence
        assertEquals(MeasureScoring.CONTINUOUSVARIABLE, result);
    }

    // ========================================
    // Tests for computeScoring(Measure, MeasureGroupComponent) - Convenience Method
    // ========================================

    @Test
    void testComputeScoring_FromMeasureAndGroup_GroupScoringPresent() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");
        measure.setScoring(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                        .setCode("proportion")));

        MeasureGroupComponent group = new MeasureGroupComponent();
        group.addExtension(
                CQFM_SCORING_EXT_URL,
                new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                                .setCode("ratio")));

        MeasureScoring result = R4MeasureUtils.computeScoring(measure, group);

        assertEquals(MeasureScoring.RATIO, result);
    }

    @Test
    void testComputeScoring_FromMeasureAndGroup_OnlyMeasureScoring() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");
        measure.setScoring(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                        .setCode("cohort")));

        MeasureGroupComponent group = new MeasureGroupComponent();

        MeasureScoring result = R4MeasureUtils.computeScoring(measure, group);

        assertEquals(MeasureScoring.COHORT, result);
    }

    @Test
    void testComputeScoring_FromMeasureAndGroup_BothAbsent_ThrowsException() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");
        // No scoring set on measure

        MeasureGroupComponent group = new MeasureGroupComponent();
        // No scoring extension on group

        InvalidRequestException exception =
                assertThrows(InvalidRequestException.class, () -> R4MeasureUtils.computeScoring(measure, group));

        assertTrue(exception.getMessage().contains("MeasureScoring must be specified"));
    }

    @Test
    void testComputeScoring_FromMeasureAndGroup_GroupOverridesMeasure() {
        Measure measure = new Measure();
        measure.setUrl("http://example.com/Measure/test");
        measure.setScoring(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                        .setCode("proportion")));

        MeasureGroupComponent group = new MeasureGroupComponent();
        group.addExtension(
                CQFM_SCORING_EXT_URL,
                new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                                .setCode("continuous-variable")));

        MeasureScoring result = R4MeasureUtils.computeScoring(measure, group);

        // Group scoring should take precedence
        assertEquals(MeasureScoring.CONTINUOUSVARIABLE, result);
    }

    // ========================================
    // Tests for getAggregateMethod
    // ========================================

    @Test
    void testGetAggregateMethod_WithSumExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("sum"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.SUM, result);
    }

    @Test
    void testGetAggregateMethod_WithAvgExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("avg"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.AVG, result);
    }

    @Test
    void testGetAggregateMethod_WithCountExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("count"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.COUNT, result);
    }

    @Test
    void testGetAggregateMethod_WithMinExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("min"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.MIN, result);
    }

    @Test
    void testGetAggregateMethod_WithMaxExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("max"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.MAX, result);
    }

    @Test
    void testGetAggregateMethod_WithMedianExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("median"));

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.MEDIAN, result);
    }

    @Test
    void testGetAggregateMethod_NoExtension() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, population);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregateMethod_NullPopulation() {
        String measureUrl = "http://example.com/Measure/test";

        ContinuousVariableObservationAggregateMethod result = R4MeasureUtils.getAggregateMethod(measureUrl, null);

        assertEquals(ContinuousVariableObservationAggregateMethod.N_A, result);
    }

    @Test
    void testGetAggregateMethod_InvalidExtensionValue() {
        String measureUrl = "http://example.com/Measure/test";
        MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
        population.addExtension(EXT_CQFM_AGGREGATE_METHOD_URL, new StringType("invalid-method"));

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class, () -> R4MeasureUtils.getAggregateMethod(measureUrl, population));

        assertTrue(exception.getMessage().contains("Aggregation method: invalid-method is not a valid value"));
        assertTrue(exception.getMessage().contains(measureUrl));
    }
}
