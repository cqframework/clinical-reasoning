package org.opencds.cqf.fhir.cr.cql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.cql.TestCql.given;
import static org.opencds.cqf.fhir.cr.library.TestLibrary.CLASS_PATH;
import static org.opencds.cqf.fhir.test.Resources.getResourcePath;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import ca.uhn.fhir.context.FhirContext;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.cql.evaluate.CqlEvaluationProcessor;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class CqlProcessorTests {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final String simpleContentDstu3 = """
        library simpleTest

        using FHIR version '3.0.1'

        context Patient

        define Test:
            5*5
        """;
    private final String simpleContentR4 = """
        library simpleTest

        using FHIR version '4.0.1'

        context Patient

        define Test:
            5*5
        """;

    @Test
    void defaultSettings() {
        var repository =
                new IgRepository(fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
        var processor = new CqlProcessor(repository);
        assertNotNull(processor.settings());
    }

    @Test
    void processor() {
        var repository =
                new IgRepository(fhirContextR4, Path.of(getResourcePath(this.getClass()) + "/" + CLASS_PATH + "/r4"));
        var processor = new CqlProcessor(
                repository,
                CrSettings.getDefault(),
                List.of(new CqlEvaluationProcessor(repository, EvaluationSettings.getDefault())));

        assertNotNull(processor.settings());
    }

    @Test
    void testDstu3() {
        var result = given().repositoryFor(fhirContextDstu3, "dstu3")
                .when()
                .subject("Patient/SimplePatient")
                .cqlContent(simpleContentDstu3)
                .thenEvaluate()
                .hasResults(2)
                .resultHasValue(1, new org.hl7.fhir.dstu3.model.IntegerType(25))
                .result;
        assertInstanceOf(org.hl7.fhir.dstu3.model.Parameters.class, result);
    }

    @Test
    void testR4() {
        var result = given().repositoryFor(fhirContextR4, "r4")
                .when()
                .subject("Patient/SimplePatient")
                .cqlContent(simpleContentDstu3)
                .thenEvaluate()
                .hasResults(2)
                .resultHasValue(1, new IntegerType(25))
                .result;
        assertInstanceOf(Parameters.class, result);
    }

    @Test
    void libraryEvaluationService_inlineAsthma() {
        var content = """
        library asthmatest version '1.0.0'

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
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .cqlContent(content)
                .thenEvaluate()
                .hasResults(3)
                .result;
        assertInstanceOf(Parameters.class, result);
    }

    @Test
    @DisplayName("Test that the content is evaluated when the repository does not contain the source CQL")
    void libraryEvaluationService_inlineAsthma_contentOnlySource() {
        var content = """
        library asthmatest version '1.0.0'

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
        var result = given().repositoryFor(fhirContextR4, "r4/libraryevalcomplexdeps")
                .when()
                .subject("Patient/SimplePatient")
                .cqlContent(content)
                .thenEvaluate()
                .hasResults(3)
                .result;
        assertInstanceOf(Parameters.class, result);
    }

    @Test
    void libraryEvaluationService_contentAndExpression() {
        var content = """
        library SimpleR4Library

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        context Patient

        define simpleBooleanExpression: true

        define observationRetrieve: [Observation]

        define observationHasCode: not IsNull(([Observation]).code)

        define "Initial Population": observationHasCode

        define "Denominator": "Initial Population"

        define "Numerator": "Denominator"
        """;
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .expression("Numerator")
                .cqlContent(content)
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);
        var results = (Parameters) result;
        assertInstanceOf(BooleanType.class, results.getParameter("Numerator").getValue());
        assertTrue(((BooleanType) results.getParameter("Numerator").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_arithmetic() {
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .expression("5*5")
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);
        var results = (Parameters) result;
        assertInstanceOf(IntegerType.class, results.getParameter("return").getValue());
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }

    @Test
    void libraryEvaluationService_paramsAndExpression() {
        var evaluationParams = parameters(datePart("%inputDate", "2019-11-01"));
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .parameters(evaluationParams)
                .expression("year from %inputDate before 2020")
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);
        var results = (Parameters) result;
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_IntegerInterval() {
        var expression = "Interval[1,5]";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .expression(expression)
                .thenEvaluate()
                .hasResults(1)
                .result;
        assertInstanceOf(Parameters.class, result);
        var report = (Parameters) result;
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("return", report.getParameterFirstRep().getName());
        var value =
                assertInstanceOf(StringType.class, report.getParameterFirstRep().getValue());
        assertEquals("Interval[1, 5]", value.toString());
    }

    @Test
    void libraryEvaluationService_Error() {
        var expression =
                "Message('Return Value If Not Error', true, 'Example Failure Code', 'Error', 'This is an error message')";
        var result = given().repositoryFor(fhirContextR4, "r4/libraryeval")
                .when()
                .expression(expression)
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
}
