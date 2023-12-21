package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

public class CqlEvaluationServiceTest {
    @Test
    void libraryEvaluationService_inlineAsthma() {
        var content = "library AsthmaTest version '1.0.0'\n" + "\n"
                + "using FHIR version '4.0.1'\n"
                + "\n"
                + "include FHIRHelpers version '4.0.1'\n"
                + "\n"
                + "codesystem \"SNOMED\": 'http://snomed.info/sct'\n"
                + "\n"
                + "code \"Asthma\": '195967001' from \"SNOMED\"\n"
                + "\n"
                + "context Patient\n"
                + "\n"
                + "define \"Asthma Diagnosis\":\n"
                + "    [Condition: \"Asthma\"]\n"
                + "\n"
                + "define \"Has Asthma Diagnosis\":\n"
                + "    exists(\"Asthma Diagnosis\")\n";
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertTrue(results.hasParameter());
        assertEquals(3, results.getParameter().size());
    }

    @Test
    void libraryEvaluationService_contentAndExpression() {
        var content = "library SimpleR4Library\n" + "\n"
                + "using FHIR version '4.0.1'\n"
                + "\n"
                + "include FHIRHelpers version '4.0.1' called FHIRHelpers\n"
                + "\n"
                + "context Patient\n"
                + "\n"
                + "define simpleBooleanExpression: true\n"
                + "\n"
                + "define observationRetrieve: [Observation]\n"
                + "\n"
                + "define observationHasCode: not IsNull(([Observation]).code)\n"
                + "\n"
                + "define \"Initial Population\": observationHasCode\n"
                + "\n"
                + "define \"Denominator\": \"Initial Population\"\n"
                + "\n"
                + "define \"Numerator\": \"Denominator\"";
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .expression("Numerator")
                .content(content)
                .evaluateCql();
        var results = when.then().parameters();
        assertFalse(results.isEmpty());
        assertEquals(1, results.getParameter().size());
        assertTrue(results.getParameter("Numerator").getValue() instanceof BooleanType);
        assertTrue(((BooleanType) results.getParameter("Numerator").getValue()).booleanValue());
        ;
    }

    @Test
    void libraryEvaluationService_arithmetic() {
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .expression("5*5")
                .evaluateCql();
        var results = when.then().parameters();
        assertTrue(results.getParameter("return").getValue() instanceof IntegerType);
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }

    @Test
    void libraryEvaluationService_paramsAndExpression() {
        Parameters evaluationParams = parameters(datePart("%inputDate", "2019-11-01"));
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .subject("Patient/SimplePatient")
                .parameters(evaluationParams)
                .expression("year from %inputDate before 2020")
                .evaluateCql();
        var results = when.then().parameters();
        assertTrue(results.getParameter("return").getValue() instanceof BooleanType);
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    @Test
    void libraryEvaluationService_ErrorLibrary() {
        var expression = "Interval[1,5]";
        var when = Library.given()
                .repositoryFor("libraryeval")
                .when()
                .expression(expression)
                .evaluateCql();
        var report = when.then().parameters();
        assertTrue(report.hasParameter());
        assertTrue(report.getParameterFirstRep().hasName());
        assertEquals("evaluation error", report.getParameterFirstRep().getName());
        assertTrue(report.getParameterFirstRep().hasResource());
        assertTrue(report.getParameterFirstRep().getResource() instanceof OperationOutcome);
        assertEquals(
                "Unsupported interval point type for FHIR conversion java.lang.Integer",
                ((OperationOutcome) report.getParameterFirstRep().getResource())
                        .getIssueFirstRep()
                        .getDetails()
                        .getText());
    }
}
