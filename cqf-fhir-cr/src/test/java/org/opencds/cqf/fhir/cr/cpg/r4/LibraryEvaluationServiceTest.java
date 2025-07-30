package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1135")
class LibraryEvaluationServiceTest {
    @Test
    void libraryEvaluationService_inlineAsthma() {
        Parameters params = parameters(stringPart("subject", "Patient/SimplePatient"));
        var libId = new IdType("Library", "asthmatest");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .parameters(params)
                .evaluateLibrary();
        var report = when.then().parameters();
        assertNotNull(report);
        assertTrue(report.hasParameter("Has Asthma Diagnosis"));
        assertTrue(((BooleanType) report.getParameter("Has Asthma Diagnosis").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_SimpleLibrary() {
        Parameters params = parameters(stringPart("subject", "Patient/SimplePatient"));
        var libId = new IdType("Library", "SimpleR4Library");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .parameters(params)
                .evaluateLibrary();
        var report = when.then().parameters();

        assertNotNull(report);
        assertTrue(report.hasParameter("Initial Population"));
        assertTrue(((BooleanType) report.getParameter("Initial Population").getValue()).booleanValue());
        assertTrue(report.hasParameter("Numerator"));
        assertTrue(((BooleanType) report.getParameter("Numerator").getValue()).booleanValue());
        assertTrue(report.hasParameter("Denominator"));
        assertTrue(((BooleanType) report.getParameter("Denominator").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_SimpleLibraryExpression() {
        List<String> expressionList = new ArrayList<>();
        expressionList.add("Numerator");

        var libId = new IdType("Library", "SimpleR4Library");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .subject("Patient/SimplePatient")
                .expressionList(expressionList)
                .evaluateLibrary();
        var report = when.then().parameters();

        assertNotNull(report);
        assertTrue(report.hasParameter("Numerator"));
        assertTrue(((BooleanType) report.getParameter("Numerator").getValue()).booleanValue());
    }

    // LUKETODO: as an experiment, try the same type of test as MultiLibEvalComplexCqlTest.java

    @Test
    void libraryEvaluationService_ErrorLibrary() {
        Parameters params = parameters(stringPart("subject", "Patient/SimplePatient"));
        var libId = new IdType("Library", "ErrorLibrary");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .parameters(params)
                .evaluateLibrary();
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
                "Exception for Library: ErrorLibrary, Message: Example Failure Code: This is an error message",
                issue.getDetails().getText().replaceAll("[\\r\\n]", ""));
    }

    @Test
    void libraryEvaluationService_ErrorPrefetchParam() {
        Parameters params = parameters(stringPart("subject", "Patient/SimplePatient"));
        var libId = new IdType("Library", "ErrorLibrary");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .prefetchData(Collections.singletonList(params))
                .evaluateLibrary();
        var report = when.then().parameters();
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("invalid parameters", report.getParameterFirstRep().getName());
        assertTrue(report.getParameterFirstRep().hasResource());
        assertTrue(report.getParameterFirstRep().getResource() instanceof OperationOutcome);
    }

    @Test
    void libraryEvaluationWithReturnedSets() {
        Parameters params = parameters(stringPart("subject", "Patient/SimplePatient"));
        var libId = new IdType("Library", "ReturnedSets");
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .id(libId)
                .parameters(params)
                .evaluateLibrary();
        var report = when.then().parameters();
        assertNotNull(report);
        assertTrue(report.hasParameter("Conditions"));
        assertTrue(report.hasParameter("Encounters"));
        assertTrue(report.hasParameter("Related Encounters"));

        var relatedEncountersByName = report.getParameter("Related Encounters").getPart().stream()
                .collect(Collectors.toMap(
                        ParametersParameterComponent::getName, ParametersParameterComponent::getResource));

        assertEquals(
                "Condition/SimpleCondition",
                relatedEncountersByName
                        .get("condition")
                        .getIdElement()
                        .toUnqualifiedVersionless()
                        .getValue());
        assertEquals(
                "Encounter/SimpleEncounter",
                relatedEncountersByName
                        .get("encounter")
                        .getIdElement()
                        .toUnqualifiedVersionless()
                        .getValue());
    }

    // ToDo: bundle test

}
