package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.stream.Collectors;

class CqlEvaluationServiceWithNpmCrossPackageTest {

    private static final Library.Given GIVEN_REPO = Library.given().repositoryPlusNpmFor("libraryevalmultilibcrosspackagenpm");

    // LUKETODO:  queries by ID are not supported for NPM
    @Test
    void crossPackageSource1() {

        // LUKETODO:  talk to JP about this:  shouyld we support an evaluate library by URL REST API?
        var params = parameters(stringPart("subject", "Patient/patient1"));
        var when = GIVEN_REPO
            .when()
            .url(new CanonicalType("http://multilib.cross.package.source.npm.opencds.org/Library/MultiLibCrossPackageSource1"))
            .parameters(params)
            .evaluateLibrary();
        // LUKETODO:  Caused by: java.lang.IllegalArgumentException: No Library found for URL: http://multilib.cross.package.source.npm.opencds.org/Library/MultiLibCrossPackageSource1
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


    // LUKETODO:  make sure you have an npm tgz in the "IG"
    @Test
    void libraryEvaluationService_inlineAsthma() {
        var content =
                """
            library opencds.multilibcrosspackagesource1.asthmatest version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            codesystem "SNOMED": 'http://snomed.info/sct'

            code "Asthma": '195967001' from "SNOMED"

            context Patient

            define "Asthma Diagnosis":
              [Condition: "Asthma"]

            define "Has Asthma Diagnosis":
              exists("Asthma Diagnosis")
        """;
        var when = GIVEN_REPO
                .when()
                .subject("Patient/SimplePatient")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertTrue(results.hasParameter());
        var parameters = results.getParameter();

        LibraryEvalTestUtils.verifyNoErrors(results);

        assertEquals(3, parameters.size());
    }

    @Test
    void libraryEvaluationService_contentAndExpression() {
        var content =
                """
        library opencds.libraryevalnpm.SimpleR4Library

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        context Patient

        define simpleBooleanExpression: true

        define observationRetrieve: [Observation]

        define observationHasCode: not IsNull(([Observation]).code)

        define "Initial Population": observationHasCode

        define "Denominator": "Initial Population"

        define "Numerator": "Denominator"";
        """;
        var when = GIVEN_REPO
                .when()
                .subject("Patient/SimplePatient")
                .expression("Numerator")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertFalse(results.isEmpty());
        assertEquals(1, results.getParameter().size());
        LibraryEvalTestUtils.verifyNoErrors(results);
        // LUKETODO:  capture the error:  "at least one libraryIdentifier Id is null"
        assertInstanceOf(BooleanType.class, results.getParameter("Numerator").getValue());
        assertTrue(((BooleanType) results.getParameter("Numerator").getValue()).booleanValue());
    }

    // LUKETODO:  this is a test with blank CQL content - is this relevant and correct?
    @Test
    void libraryEvaluationService_arithmetic() {
        var when = GIVEN_REPO.when().expression("5*5").evaluateCql();
        var results = when.then().parameters();
        assertInstanceOf(IntegerType.class, results.getParameter("return").getValue());
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }

    @Test
    void libraryEvaluationService_paramsAndExpression() {
        Parameters evaluationParams = parameters(datePart("%inputDate", "2019-11-01"));
        var when = GIVEN_REPO
                .when()
                .subject("Patient/SimplePatient")
                .parameters(evaluationParams)
                .expression("year from %inputDate before 2020")
                .evaluateCql();
        var results = when.then().parameters();
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    // LUKETODO:  also testing blank CQL content here - is this relevant and correct?
    @Test
    void libraryEvaluationService_IntegerInterval() {
        var expression = "Interval[1,5]";
        var when = GIVEN_REPO.when().expression(expression).evaluateCql();
        var report = when.then().parameters();
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("return", report.getParameterFirstRep().getName());
        var value =
                assertInstanceOf(StringType.class, report.getParameterFirstRep().getValue());
        assertEquals("Interval[1, 5]", value.toString());
    }

    // LUKETODO: also testing blank CQL content here - is this relevant and correct?
    @Test
    void libraryEvaluationService_Error() {
        var expression =
                "Message('Return Value If Not Error', true, 'Example Failure Code', 'Error', 'This is an error message')";

        var when = GIVEN_REPO.when().expression(expression).evaluateCql();
        var report = when.then().parameters();
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("evaluation error", report.getParameterFirstRep().getName());
        var outcome = assertInstanceOf(
                OperationOutcome.class, report.getParameterFirstRep().getResource());
        assertFalse(outcome.getIssue().isEmpty());
        var issue = outcome.getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertEquals(
                "Example Failure Code: This is an error message",
                issue.getDetails().getText().replaceAll("[\\r\\n]", ""));
    }
}
