package org.opencds.cqf.fhir.cr.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.given;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1135")
class LibraryEvaluationServiceTest {
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();

    @Test
    void libraryEvaluationService_inlineAsthma() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/SimplePatient"));
        var libId = "asthmatest";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(3)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
        assertNotNull(report);
        assertTrue(report.hasParameter("Has Asthma Diagnosis"));
        assertTrue(((BooleanType) report.getParameter("Has Asthma Diagnosis").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_SimpleLibrary() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/SimplePatient"));
        var libId = "SimpleR4Library";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(7)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
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
        var expressionList = new ArrayList<String>();
        expressionList.add("Numerator");

        var libId = "SimpleR4Library";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .libraryId(libId)
                .subjectId("Patient/SimplePatient")
                .expression(expressionList)
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
        assertNotNull(report);
        assertTrue(report.hasParameter("Numerator"));
        assertTrue(((BooleanType) report.getParameter("Numerator").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_ErrorLibrary() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/SimplePatient"));
        var libId = "ErrorLibrary";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
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
                issue.getDiagnostics().replaceAll("[\\r\\n]", ""));
    }

    @Test
    void libraryEvaluationWithReturnedSets() {
        var params = newParameters(fhirContextR4, newStringPart(fhirContextR4, "subject", "Patient/SimplePatient"));
        var libId = "ReturnedSets";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .libraryId(libId)
                .parameters(params)
                .thenEvaluate()
                .hasResults(4)
                .result;
        assertInstanceOf(Parameters.class, result);

        var report = (Parameters) result;
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
