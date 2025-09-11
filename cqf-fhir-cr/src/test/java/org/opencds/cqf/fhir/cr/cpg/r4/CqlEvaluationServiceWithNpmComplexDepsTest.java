package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.*;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import java.util.Collection;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;

class CqlEvaluationServiceWithNpmComplexDepsTest {

    private static final Library.Given GIVEN_REPO = Library.given().repositoryPlusNpmFor("libraryevalcomplexdepsnpm");

    // LUKETODO:  reference another CQL here
    @Test
    void libraryEvaluationService_inlineAsthma_npm() {
        var content =
                """
            library opencds.libraryevalnpm.asthmatest version '1.0.0'

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
