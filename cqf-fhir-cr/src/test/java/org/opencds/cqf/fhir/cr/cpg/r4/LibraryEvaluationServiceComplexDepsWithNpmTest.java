package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1135")
class LibraryEvaluationServiceComplexDepsWithNpmTest {

    private static final Library.Given GIVEN_REPO = Library.given().repositoryPlusNpmFor("libraryevalcomplexdepsnpm");

    // LUKETODO:  queries by ID are not supported for NPM
    @Test
    void singleLibraryTest_1A() {

        // LUKETODO:  talk to JP about this:  shouyld we support an evaluate library by URL REST API?
        var params = parameters(stringPart("subject", "Patient/patient1"));
        var libId = new IdType("Library", "Level1A");
        var when = GIVEN_REPO
                .when()
                .url(new CanonicalType("http://multilib.complexdeps.npm.opencds.org/Library/Level1A"))
                .parameters(params)
                .evaluateLibrary();
        var report = when.then().parameters();

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
        var params = parameters(stringPart("subject", "Patient/patient1"));
        var libId = new IdType("Library", "Level1B");
        var when = GIVEN_REPO.when().id(libId).parameters(params).evaluateLibrary();
        var report = when.then().parameters();

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
