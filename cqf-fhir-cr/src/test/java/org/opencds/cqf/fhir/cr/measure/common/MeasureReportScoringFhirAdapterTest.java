package org.opencds.cqf.fhir.cr.measure.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Minimal unit tests for MeasureReportScoringFhirAdapter.
 * Tests verify the external API for post-hoc MeasureReport scoring.
 *
 * <p>Part 1 - Phase 7: These minimal tests verify the adapter infrastructure works.
 * Comprehensive testing with actual measures will be done in Part 2.
 */
class MeasureReportScoringFhirAdapterTest {

    // ========== Common Constants ==========

    private static final String MEASURE_SCORING_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-scoring";
    private static final String MEASURE_POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String MEASURE_URL = "http://example.com/Measure/test";
    private static final String MEASURE_ID = "test";

    @Test
    void testScore_NullMeasure_ThrowsException() {
        // Given: Null measure
        MeasureReport report = new MeasureReport();

        // When/Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MeasureReportScoringFhirAdapter.score(null, report);
        });

        assertEquals("Measure cannot be null", exception.getMessage());
    }

    @Test
    void testScore_NullMeasureReport_ThrowsException() {
        // Given: Null report
        Measure measure = new Measure();

        // When/Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MeasureReportScoringFhirAdapter.score(measure, null);
        });

        assertEquals("MeasureReport cannot be null", exception.getMessage());
    }

    @Test
    void testScore_MismatchedVersions_ThrowsException() {
        // Given: R4 Measure and DSTU3 MeasureReport (mismatched)
        Measure r4Measure = new Measure().setUrl(MEASURE_URL);

        org.hl7.fhir.dstu3.model.MeasureReport dstu3Report = new org.hl7.fhir.dstu3.model.MeasureReport();

        // When/Then: Should throw UnprocessableEntityException
        assertThrows(UnprocessableEntityException.class, () -> {
            MeasureReportScoringFhirAdapter.score(r4Measure, dstu3Report);
        });
    }

    // ========== Common Helper Methods ==========

    /**
     * Create a Coding for measure scoring (e.g., "proportion").
     */
    private static Coding createMeasureScoringCoding(String code) {
        return new Coding().setSystem(MEASURE_SCORING_SYSTEM).setCode(code);
    }

    /**
     * Create a Coding for measure population (e.g., "initial-population", "denominator", "numerator").
     */
    private static Coding createMeasurePopulationCoding(String code) {
        return new Coding().setSystem(MEASURE_POPULATION_SYSTEM).setCode(code);
    }

    @Nested
    class R4Tests {

        @Test
        void testScore_BasicProportionMeasure() {
            // Given: Simple R4 proportion measure and report with counts (75/100)
            Measure measure = createSimpleR4ProportionMeasure();
            MeasureReport report = createSimpleR4MeasureReportWithCounts(100, 75);

            // When: Score the report
            MeasureReportScoringFhirAdapter.score(measure, report);

            // Then: Score should be calculated and copied to the report
            assertNotNull(report, "Report should not be null after scoring");
            assertNotNull(report.getGroup(), "Report should have groups");
            assertEquals(1, report.getGroup().size(), "Report should have 1 group");

            // Verify measure score was calculated (75/100 = 0.75)
            MeasureReportGroupComponent group = report.getGroup().get(0);
            assertNotNull(group.getMeasureScore(), "Group should have a measure score");
            assertEquals(0.75, group.getMeasureScore().getValue().doubleValue(), 0.0001, "Score should be 0.75");
        }

        @Test
        void testScore_ProportionMeasureWithDifferentCounts() {
            // Given: R4 proportion measure and report with different counts (42/200)
            Measure measure = createSimpleR4ProportionMeasure();
            MeasureReport report = createSimpleR4MeasureReportWithCounts(200, 42);

            // When: Score the report
            MeasureReportScoringFhirAdapter.score(measure, report);

            // Then: Score should be calculated and copied to the report (42/200 = 0.21)
            assertNotNull(report, "Report should not be null after scoring");
            assertNotNull(report.getGroup(), "Report should have groups");
            assertEquals(1, report.getGroup().size(), "Report should have 1 group");

            MeasureReportGroupComponent group = report.getGroup().get(0);
            assertNotNull(group.getMeasureScore(), "Group should have a measure score");
            assertEquals(0.21, group.getMeasureScore().getValue().doubleValue(), 0.0001, "Score should be 0.21");
        }

        // ========== R4 Helper Methods ==========

        private Measure createSimpleR4ProportionMeasure() {
            Measure measure = new Measure();
            measure.setId(MEASURE_ID);
            measure.setUrl(MEASURE_URL);
            measure.setScoring(new CodeableConcept().addCoding(createMeasureScoringCoding("proportion")));

            // Add a simple group with populations
            MeasureGroupComponent group = measure.addGroup();
            group.addPopulation(createR4MeasurePopulation("initial-population"));
            group.addPopulation(createR4MeasurePopulation("denominator"));
            group.addPopulation(createR4MeasurePopulation("numerator"));

            return measure;
        }

        /**
         * Create a MeasureGroupPopulationComponent with the given population code.
         */
        private MeasureGroupPopulationComponent createR4MeasurePopulation(String populationCode) {
            MeasureGroupPopulationComponent population = new MeasureGroupPopulationComponent();
            population.setId(populationCode);
            population.setCode(new CodeableConcept().addCoding(createMeasurePopulationCoding(populationCode)));
            return population;
        }

        private MeasureReport createSimpleR4MeasureReportWithCounts(int denominator, int numerator) {
            MeasureReport report = new MeasureReport();
            report.setMeasure(MEASURE_URL);

            MeasureReportGroupComponent group = report.addGroup();
            group.addPopulation(createR4ReportPopulation("initial-population", denominator));
            group.addPopulation(createR4ReportPopulation("denominator", denominator));
            group.addPopulation(createR4ReportPopulation("numerator", numerator));

            return report;
        }

        /**
         * Create a MeasureReportGroupPopulationComponent with the given code and count.
         */
        private MeasureReportGroupPopulationComponent createR4ReportPopulation(String populationCode, int count) {
            return new MeasureReportGroupPopulationComponent()
                    .setCode(new CodeableConcept().addCoding(createMeasurePopulationCoding(populationCode)))
                    .setCount(count);
        }
    }

    @Nested
    class Dstu3Tests {

        @Test
        void testScore_BasicProportionMeasure() {
            // Given: Simple DSTU3 proportion measure and report with counts (80/100)
            org.hl7.fhir.dstu3.model.Measure measure = createSimpleDstu3ProportionMeasure();
            org.hl7.fhir.dstu3.model.MeasureReport report = createSimpleDstu3MeasureReportWithCounts(100, 80);

            // When: Score the report
            MeasureReportScoringFhirAdapter.score(measure, report);

            // Then: Score should be calculated and copied to the report (80/100 = 0.80)
            assertNotNull(report, "Report should not be null after scoring");
            assertNotNull(report.getGroup(), "Report should have groups");
            assertEquals(1, report.getGroup().size(), "Report should have 1 group");

            org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group =
                    report.getGroup().get(0);
            assertNotNull(group.getMeasureScore(), "Group should have a measure score");
            assertEquals(0.80, group.getMeasureScore().doubleValue(), 0.0001, "Score should be 0.80");
        }

        @Test
        void testScore_ProportionMeasureWithDifferentCounts() {
            // Given: DSTU3 proportion measure and report with different counts (150/250)
            org.hl7.fhir.dstu3.model.Measure measure = createSimpleDstu3ProportionMeasure();
            org.hl7.fhir.dstu3.model.MeasureReport report = createSimpleDstu3MeasureReportWithCounts(250, 150);

            // When: Score the report
            MeasureReportScoringFhirAdapter.score(measure, report);

            // Then: Score should be calculated and copied to the report (150/250 = 0.60)
            assertNotNull(report, "Report should not be null after scoring");
            assertNotNull(report.getGroup(), "Report should have groups");
            assertEquals(1, report.getGroup().size(), "Report should have 1 group");

            org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group =
                    report.getGroup().get(0);
            assertNotNull(group.getMeasureScore(), "Group should have a measure score");
            assertEquals(0.60, group.getMeasureScore().doubleValue(), 0.0001, "Score should be 0.60");
        }

        // ========== DSTU3 Helper Methods ==========

        private org.hl7.fhir.dstu3.model.Measure createSimpleDstu3ProportionMeasure() {
            org.hl7.fhir.dstu3.model.Measure measure = new org.hl7.fhir.dstu3.model.Measure();
            measure.setId(MEASURE_ID);
            measure.setUrl(MEASURE_URL);
            measure.setScoring(new org.hl7.fhir.dstu3.model.CodeableConcept()
                    .addCoding(createDstu3MeasureScoringCoding("proportion")));

            // Add a simple group with populations
            org.hl7.fhir.dstu3.model.Measure.MeasureGroupComponent group = measure.addGroup();
            group.addPopulation(createDstu3MeasurePopulation("initial-population"));
            group.addPopulation(createDstu3MeasurePopulation("denominator"));
            group.addPopulation(createDstu3MeasurePopulation("numerator"));

            return measure;
        }

        /**
         * Create a DSTU3 Coding for measure scoring.
         */
        private org.hl7.fhir.dstu3.model.Coding createDstu3MeasureScoringCoding(String code) {
            return new org.hl7.fhir.dstu3.model.Coding()
                    .setSystem(MEASURE_SCORING_SYSTEM)
                    .setCode(code);
        }

        /**
         * Create a DSTU3 Coding for measure population.
         */
        private org.hl7.fhir.dstu3.model.Coding createDstu3MeasurePopulationCoding(String code) {
            return new org.hl7.fhir.dstu3.model.Coding()
                    .setSystem(MEASURE_POPULATION_SYSTEM)
                    .setCode(code);
        }

        /**
         * Create a DSTU3 MeasureGroupPopulationComponent with the given population code.
         */
        private org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent createDstu3MeasurePopulation(
                String populationCode) {
            org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent population =
                    new org.hl7.fhir.dstu3.model.Measure.MeasureGroupPopulationComponent();
            population.setId(populationCode);
            population.setCode(new org.hl7.fhir.dstu3.model.CodeableConcept()
                    .addCoding(createDstu3MeasurePopulationCoding(populationCode)));
            return population;
        }

        private org.hl7.fhir.dstu3.model.MeasureReport createSimpleDstu3MeasureReportWithCounts(
                int denominator, int numerator) {
            org.hl7.fhir.dstu3.model.MeasureReport report = new org.hl7.fhir.dstu3.model.MeasureReport();
            report.setMeasure(new org.hl7.fhir.dstu3.model.Reference(MEASURE_URL));

            org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupComponent group = report.addGroup();
            group.addPopulation(createDstu3ReportPopulation("initial-population", denominator));
            group.addPopulation(createDstu3ReportPopulation("denominator", denominator));
            group.addPopulation(createDstu3ReportPopulation("numerator", numerator));

            return report;
        }

        /**
         * Create a DSTU3 MeasureReportGroupPopulationComponent with the given code and count.
         */
        private org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent
                createDstu3ReportPopulation(String populationCode, int count) {
            return new org.hl7.fhir.dstu3.model.MeasureReport.MeasureReportGroupPopulationComponent()
                    .setCode(new org.hl7.fhir.dstu3.model.CodeableConcept()
                            .addCoding(createDstu3MeasurePopulationCoding(populationCode)))
                    .setCount(count);
        }
    }
}
