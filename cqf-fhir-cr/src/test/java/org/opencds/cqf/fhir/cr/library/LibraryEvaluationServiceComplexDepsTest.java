package org.opencds.cqf.fhir.cr.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.given;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1135")
class LibraryEvaluationServiceComplexDepsTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void singleLibraryTest_1A() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/patient1"));
        var libId = "Level1A";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryevalcomplexdeps")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(21)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
        assertNotNull(report);
        assertTrue(report.hasParameter("Initial Population"));
        assertTrue(report.hasParameter("Numerator"));
        assertTrue(report.hasParameter("Denominator"));

        var initialPopulations = report.getParameters("Initial Population");
        assertNotNull(initialPopulations);
        assertFalse(initialPopulations.isEmpty());
        assertEquals(10, initialPopulations.size());

        initialPopulations.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Initial Population", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedInitialPopulationEncounterIds = Set.of(
                "enc_planned_cat1",
                "enc_cancelled_cat3",
                "enc_planned_cat2",
                "enc_triaged_cat3",
                "enc_planned_cat3",
                "enc_arrived_cat1_v2",
                "enc_arrived_cat3",
                "enc_arrived_cat1_v1",
                "enc_arrived_cat2",
                "enc_finished_cat3");

        assertEquals(
                expectedInitialPopulationEncounterIds,
                initialPopulations.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));

        var denominators = report.getParameters("Denominator");
        assertNotNull(denominators);
        assertFalse(denominators.isEmpty());
        assertEquals(7, denominators.size());

        denominators.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Denominator", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedDenominatorEncounterIds = Set.of(
                "enc_planned_cat1",
                "enc_planned_cat2",
                "enc_planned_cat3",
                "enc_arrived_cat1_v2",
                "enc_arrived_cat3",
                "enc_arrived_cat1_v1",
                "enc_arrived_cat2");

        assertEquals(
                expectedDenominatorEncounterIds,
                denominators.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));

        var numerators = report.getParameters("Numerator");
        assertNotNull(numerators);
        assertFalse(numerators.isEmpty());
        assertEquals(3, numerators.size());

        numerators.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Numerator", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedNumeratorEncounterIds = Set.of("enc_planned_cat1", "enc_planned_cat2", "enc_planned_cat3");

        assertEquals(
                expectedNumeratorEncounterIds,
                numerators.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));
    }

    @Test
    void singleLibraryTest_1B() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/patient1"));
        var libId = "Level1B";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryevalcomplexdeps")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(14)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
        assertNotNull(report);
        assertTrue(report.hasParameter("Initial Population"));
        assertTrue(report.hasParameter("Numerator"));
        assertTrue(report.hasParameter("Denominator"));

        var initialPopulations = report.getParameters("Initial Population");
        assertNotNull(initialPopulations);
        assertFalse(initialPopulations.isEmpty());
        assertEquals(10, initialPopulations.size());

        initialPopulations.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Initial Population", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedInitialPopulationEncounterIds = Set.of(
                "enc_planned_cat1",
                "enc_cancelled_cat3",
                "enc_planned_cat2",
                "enc_triaged_cat3",
                "enc_planned_cat3",
                "enc_arrived_cat1_v2",
                "enc_arrived_cat3",
                "enc_arrived_cat1_v1",
                "enc_arrived_cat2",
                "enc_finished_cat3");

        assertEquals(
                expectedInitialPopulationEncounterIds,
                initialPopulations.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));

        var denominators = report.getParameters("Denominator");
        assertNotNull(denominators);
        assertFalse(denominators.isEmpty());
        assertEquals(3, denominators.size());

        denominators.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Denominator", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedDenominatorEncounterIds = Set.of("enc_planned_cat1", "enc_arrived_cat1_v2", "enc_arrived_cat1_v1");

        assertEquals(
                expectedDenominatorEncounterIds,
                denominators.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));

        var numerators = report.getParameters("Numerator");
        assertNotNull(numerators);
        assertFalse(numerators.isEmpty());
        assertEquals(1, numerators.size());

        numerators.forEach(component -> {
            assertNotNull(component);
            assertNull(component.getValue());
            assertEquals("Numerator", component.getName());
            assertInstanceOf(Encounter.class, component.getResource());
        });

        var expectedNumeratorEncounterIds = Set.of("enc_planned_cat1");

        assertEquals(
                expectedNumeratorEncounterIds,
                numerators.stream()
                        .map(Parameters.ParametersParameterComponent::getResource)
                        .map(r -> r.getIdElement().getIdPart())
                        .collect(Collectors.toUnmodifiableSet()));
    }
}
