package org.opencds.cqf.fhir.cr.measure.r4;

import static java.util.function.Predicate.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupStratifierComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.StratifierGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4DateHelper;
import org.opencds.cqf.fhir.utility.r4.Parameters;

class MeasureProcessorEvaluateTest {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    protected static Given given = Measure.given().repositoryFor("CaseRepresentation101");

    @Test
    void measure_eval() {

        var start = LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault());
        var end = LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneId.systemDefault());
        var helper = new R4DateHelper();
        var measurementPeriod = helper.buildMeasurementPeriod(start, end);
        var report = given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart(start)
                .periodEnd(end)
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .reportType("subject")
                .evaluate()
                .then()
                .report();

        assertEquals(
                measurementPeriod.getStart().toInstant(),
                report.getPeriod().getStart().toInstant());
        assertEquals(
                measurementPeriod.getEnd().toInstant(),
                report.getPeriod().getEnd().toInstant());
    }

    @Test
    void measure_eval_UTC() {
        var start = LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay(ZoneOffset.UTC);
        var end = LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneOffset.UTC);
        var helper = new R4DateHelper();
        var measurementPeriod = helper.buildMeasurementPeriod(start, end);
        var report = given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart(start)
                .periodEnd(end)
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .reportType("subject")
                .evaluate()
                .then()
                .report();

        final Period actualPeriod = report.getPeriod();

        assertEquals(
                measurementPeriod.getStart().toInstant(),
                actualPeriod.getStart().toInstant());
        assertEquals(
                measurementPeriod.getEnd().toInstant(), actualPeriod.getEnd().toInstant());
    }

    @Test
    void measure_eval_with_additional_data() {
        Bundle additionalData = (Bundle) FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(
                        MeasureProcessorEvaluateTest.class.getResourceAsStream("CaseRepresentation101/generated.json"));
        var report = given.when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-01-31")
                .subject("Patient/980babd9-4979-4b76-978c-946719022dbb")
                .additionalData(additionalData)
                .evaluate()
                .then()
                .hasMeasureVersion("0.000.01")
                .report();

        assertEquals("2022-01-01", formatter.format(report.getPeriod().getStart()));
        assertEquals("2022-01-31", formatter.format(report.getPeriod().getEnd()));
    }

    private static Stream<Arguments> parametersTestParams() {
        return Stream.of(
                Arguments.of(null, 0, 0),
                Arguments.of(
                        Parameters.parameters(Parameters.part("encounter", "2d0ecfb4-9dec-4daa-a261-e37e426d0d7b")),
                        1,
                        1));
    }

    @ParameterizedTest
    @MethodSource("parametersTestParams")
    void measure_eval_with_parameters_mega(
            @Nullable org.hl7.fhir.r4.model.Parameters parameters, int expectedMeasureScore, int expectedMeasureCount) {
        // This test should fail on numerator due to the encounter parameter not being set
        var when = Measure.given()
                .repositoryFor("ANC")
                .when()
                .measureId("ANCIND01")
                .subject("Patient/457865b6-8f02-49e2-8a77-21b73eb266d4")
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("subject")
                .parameters(parameters)
                .evaluate();
        MeasureReport report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        var group = report.getGroupFirstRep();
        assertEquals(expectedMeasureScore, group.getMeasureScore().getValue().intValue());
        group.getPopulation().forEach(population -> {
            assertPopulation(expectedMeasureCount, population);
        });

        var stratifiers = group.getStratifier();

        assertStratifiers(expectedMeasureScore, expectedMeasureCount, stratifiers);
    }

    private static Stream<Arguments> simpleParametersParams2() {
        return Stream.of(
                Arguments.of(null, 0),
                Arguments.of(Parameters.parameters(Parameters.part("practitionerParam", "bogus")), 0),
                Arguments.of(
                        Parameters.parameters(
                                Parameters.part("bogusPractitionerParam", "simpleCqlParamsPractitioner1")),
                        0),
                Arguments.of(Parameters.parameters(Parameters.part("encounterParam", "bogus")), 0),
                Arguments.of(
                        Parameters.parameters(Parameters.part("bogusEncounterParam", "simpleCqlParamsEncounter1")), 0),
                Arguments.of(
                        Parameters.parameters(Parameters.part("practitionerParam", "simpleCqlParamsPractitioner1")), 1),
                Arguments.of(Parameters.parameters(Parameters.part("encounterParam", "simpleCqlParamsEncounter1")), 1));
    }

    @ParameterizedTest
    @MethodSource("simpleParametersParams2")
    void simpleParameters(@Nullable org.hl7.fhir.r4.model.Parameters parameters, int expectedMeasureCount) {
        var when = Measure.given()
                .repositoryFor("SimpleCqlParameters")
                .when()
                .measureId("simpleCqlParameters")
                .subject("Patient/simpleCqlParamsPatient1")
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("subject")
                .parameters(parameters)
                .evaluate();

        var report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());

        var group = report.getGroupFirstRep();

        group.getPopulation().forEach(population -> {
            assertPopulation(expectedMeasureCount, population);
        });
    }

    private void assertPopulation(
            int expectedMeasureCount, MeasureReport.MeasureReportGroupPopulationComponent population) {
        assertTrue(population.hasCount());
        assertTrue(population.hasCode());
        assertTrue(population.getCode().hasCoding());
        assertTrue(population.getCode().getCodingFirstRep().hasCode());
        String code = population.getCode().getCodingFirstRep().getCode();
        switch (code) {
            case "initial-population":
            case "denominator":
                assertEquals(1, population.getCount());
                break;
            case "numerator":
                assertEquals(expectedMeasureCount, population.getCount());
                break;
        }
    }

    private void assertStratifiers(
            int expectedMeasureScore,
            int expectedMeasureCount,
            List<MeasureReportGroupStratifierComponent> stratifiers) {

        var stratifierGroupComponents = stratifiers.stream()
                .map(MeasureReportGroupStratifierComponent::getStratum)
                .filter(not(Collection::isEmpty))
                .map(strat -> strat.get(0))
                .toList();

        var stratumScores = stratifierGroupComponents.stream()
                .map(StratifierGroupComponent::getMeasureScore)
                .map(Quantity::getValue)
                .map(BigDecimal::intValue)
                .toList();

        assertEquals(1, stratumScores.size());
        assertEquals(expectedMeasureScore, stratumScores.get(0));

        var stratCountByPopType = stratifierGroupComponents.stream()
                .map(StratifierGroupComponent::getPopulation)
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        pop -> pop.getCode().getCodingFirstRep().getCode(),
                        StratifierGroupPopulationComponent::getCount));

        assertEquals(1, stratCountByPopType.get("initial-population"));
        assertEquals(1, stratCountByPopType.get("denominator"));
        assertEquals(expectedMeasureCount, stratCountByPopType.get("numerator"));
    }

    @Test
    void with_custom_options() {
        var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
        evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
        evaluationOptions
                .getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationOptions
                .getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

        var when = Measure.given()
                .repositoryFor("CaseRepresentation101")
                .evaluationOptions(evaluationOptions)
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .periodStart("2022-01-01")
                .periodEnd("2022-06-29")
                .reportType("subject")
                .evaluate();

        assertNotNull(when.then().report());

        // Run again to find any issues with caching
        when.then().report();
    }

    @Test
    void additional_data_with_custom_options() {
        var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
        evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
        evaluationOptions
                .getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);

        evaluationOptions
                .getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

        Bundle additionalData = (Bundle) FhirContext.forR4Cached()
                .newJsonParser()
                .parseResource(
                        MeasureProcessorEvaluateTest.class.getResourceAsStream("CaseRepresentation101/generated.json"));

        var when = Measure.given()
                .repositoryFor("CaseRepresentation101")
                .evaluationOptions(evaluationOptions)
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .periodStart("2022-01-01")
                .periodEnd("2022-01-31")
                .subject("Patient/980babd9-4979-4b76-978c-946719022dbb")
                .additionalData(additionalData)
                .evaluate();

        assertNotNull(when.then().report());

        // Run again to find any issues with caching
        when.then().report();
    }
    /**
     * test to validate that measure with MeasureScorer specified at the group level
     * and nothing on measure-level MeasureScorer
     */
    @Test
    void measure_eval_group_measurescorer() {
        var when = Measure.given()
                .repositoryFor("DischargedonAntithromboticTherapyFHIR")
                .when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();
        MeasureReport report = when.then().report();
        assertNotNull(report);
        assertEquals(1, report.getGroup().size());
        assertEquals(3, report.getGroupFirstRep().getPopulation().get(0).getCount());
    }

    @Test
    void measure_eval_group_measurescorer_invalidMeasureScore() {
        // Removed MeasureScorer from Measure, should trigger exception
        var when = Measure.given()
                .repositoryFor("InvalidMeasure")
                .when()
                .measureId("DischargedonAntithromboticTherapyFHIR")
                .subject(null)
                .periodStart("2018-01-01")
                .periodEnd("2030-12-31")
                .reportType("population")
                .evaluate();

        String errorMsg =
                "MeasureScoring must be specified on Group or Measure for Measure: https://madie.cms.gov/Measure/DischargedonAntithromboticTherapyFHIR";
        var e = assertThrows(InvalidRequestException.class, when::then);
        assertEquals(errorMsg, e.getMessage());
    }
}
