package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Nullable;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.utility.r4.Parameters;

public class MeasureProcessorCQLParameterTest {

    private static Stream<Arguments> simpleParametersParams() {
        return Stream.of(
                Arguments.of(null, 0),
                Arguments.of(Parameters.parameters(Parameters.part("practitionerParam", "bogus")), 0),
                Arguments.of(
                        Parameters.parameters(
                                Parameters.part("bogusPractitionerParam", "SimpleCqlParamsPractitioner1")),
                        0),
                Arguments.of(Parameters.parameters(Parameters.part("encounterParam", "bogus")), 0),
                Arguments.of(
                        Parameters.parameters(Parameters.part("bogusEncounterParam", "SimpleCqlParamsEncounter1")), 0),
                Arguments.of(
                        Parameters.parameters(Parameters.part("practitionerParam", "SimpleCqlParamsPractitioner1")), 1),
                Arguments.of(Parameters.parameters(Parameters.part("encounterParam", "SimpleCqlParamsEncounter1")), 1));
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("simpleParametersParams")
    void simpleParameters(@Nullable org.hl7.fhir.r4.model.Parameters parameters, int expectedMeasureCount) {
        var when = Measure.given()
                .repositoryFor("MeasureTest")
                .when()
                .measureId("SimpleCqlParameters")
                .subject("Patient/SimpleCqlParamsPatient1")
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
}
