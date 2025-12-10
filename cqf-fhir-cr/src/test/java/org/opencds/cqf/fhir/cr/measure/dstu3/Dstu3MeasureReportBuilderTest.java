package org.opencds.cqf.fhir.cr.measure.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureReportType;

/**
 * Minimal unit tests for Dstu3MeasureReportBuilder score copying logic.
 * Tests verify that scores set on Def objects are correctly copied to DSTU3 MeasureReports.
 *
 * <p>Part 1 - Phase 7: These tests verify the score copying infrastructure works,
 * even though full scoring integration happens in Part 2.
 */
class Dstu3MeasureReportBuilderTest {

    public static final String MEASURE_ID_1 = "measure1";
    public static final String MEASURE_URL_1 = "http://something.com/measure1";

    // ========== Score Copying Tests (Part 1 - Phase 7) ==========

    @Test
    void testScoreCopying_GroupScore() {
        // Given: Measure and MeasureDef built from it
        Measure measure = buildSimpleProportionMeasure(MEASURE_ID_1, MEASURE_URL_1);
        var dstu3MeasureDefBuilder = new Dstu3MeasureDefBuilder();
        MeasureDef measureDef = dstu3MeasureDefBuilder.build(measure);

        // Manually set a score on the first group
        measureDef.groups().get(0).setScore(0.80);

        // When: Build the MeasureReport
        var dstu3MeasureReportBuilder = new Dstu3MeasureReportBuilder();
        var measureReport =
                dstu3MeasureReportBuilder.build(measure, measureDef, MeasureReportType.INDIVIDUAL, null, List.of());

        // Then: Group score should be copied to the report
        assertNotNull(measureReport);
        assertNotNull(measureReport.getGroup());
        assertEquals(1, measureReport.getGroup().size());

        var reportGroup = measureReport.getGroup().get(0);
        assertTrue(reportGroup.hasMeasureScore(), "Group should have a measure score");
        assertEquals(0.80, reportGroup.getMeasureScore().doubleValue(), 0.001);
    }

    @Test
    void testScoreCopying_NullScore() {
        // Given: Measure and MeasureDef with null score (e.g., COHORT measure)
        Measure measure = buildSimpleProportionMeasure(MEASURE_ID_1, MEASURE_URL_1);
        var dstu3MeasureDefBuilder = new Dstu3MeasureDefBuilder();
        MeasureDef measureDef = dstu3MeasureDefBuilder.build(measure);

        // Explicitly set null score (or just don't set it)
        measureDef.groups().get(0).setScore(null);

        // When: Build the MeasureReport
        var dstu3MeasureReportBuilder = new Dstu3MeasureReportBuilder();
        var measureReport =
                dstu3MeasureReportBuilder.build(measure, measureDef, MeasureReportType.INDIVIDUAL, null, List.of());

        // Then: No score should be set in the report
        assertNotNull(measureReport);
        var reportGroup = measureReport.getGroup().get(0);
        assertNull(reportGroup.getMeasureScore(), "Null score should not be copied");
    }

    @Test
    void testScoreCopying_NegativeScore() {
        // Given: Measure and MeasureDef with negative score (error indicator)
        Measure measure = buildSimpleProportionMeasure(MEASURE_ID_1, MEASURE_URL_1);
        var dstu3MeasureDefBuilder = new Dstu3MeasureDefBuilder();
        MeasureDef measureDef = dstu3MeasureDefBuilder.build(measure);

        // Set negative score
        measureDef.groups().get(0).setScore(-1.0);

        // When: Build the MeasureReport
        var dstu3MeasureReportBuilder = new Dstu3MeasureReportBuilder();
        var measureReport =
                dstu3MeasureReportBuilder.build(measure, measureDef, MeasureReportType.INDIVIDUAL, null, List.of());

        // Then: Negative score should not be copied (filtered by >= 0 check)
        assertNotNull(measureReport);
        var reportGroup = measureReport.getGroup().get(0);
        assertNull(reportGroup.getMeasureScore(), "Negative score should not be copied");
    }

    // ========== Helper Methods ==========

    private static Measure buildSimpleProportionMeasure(String id, String url) {
        Measure measure = new Measure();
        measure.setId(id);
        measure.setUrl(url);
        measure.setScoring(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
                        .setCode("proportion")));

        // Add a simple group with basic populations
        MeasureGroupComponent group = measure.addGroup();

        MeasureGroupPopulationComponent initialPop = new MeasureGroupPopulationComponent();
        initialPop.setId("initial-population");
        initialPop.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                        .setCode("initial-population")));
        group.addPopulation(initialPop);

        MeasureGroupPopulationComponent denom = new MeasureGroupPopulationComponent();
        denom.setId("denominator");
        denom.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                        .setCode("denominator")));
        group.addPopulation(denom);

        MeasureGroupPopulationComponent numer = new MeasureGroupPopulationComponent();
        numer.setId("numerator");
        numer.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
                        .setCode("numerator")));
        group.addPopulation(numer);

        return measure;
    }
}
