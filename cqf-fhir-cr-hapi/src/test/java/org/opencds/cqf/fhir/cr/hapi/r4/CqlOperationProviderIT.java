package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.utility.r4.Parameters.booleanPart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.canonicalPart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.datePart;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;
import static org.opencds.cqf.fhir.utility.r4.Parameters.stringPart;

import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CqlOperationProviderIT extends BaseCrR4TestServer {
    @BeforeEach
    void setup() {
        var requestDetails = setupRequestDetails();
        loadResource(Library.class, "SimpleR4Library.json", requestDetails);
        loadResource(Patient.class, "SimplePatient.json", requestDetails);
        loadResource(Observation.class, "SimpleObservation.json", requestDetails);
        loadResource(Condition.class, "SimpleCondition.json", requestDetails);
    }

    @Test
    void cqlProviderTest() {
        // reuse loaded resources for all tests
        assertTrue(cqlExecutionProviderTestSimpleDate());
        cqlExecutionProviderTestSimpleArithmetic();
        cqlExecutionProviderTestReferencedLibrary();
        cqlExecutionProviderTestDataBundle();
        cqlExecutionProviderTestDataBundleWithSubject();
        cqlExecutionProviderTestSimpleParameters();
        cqlExecutionProviderTestExpression();
        cqlExecutionProviderTestErrorExpression();
        cqlExecutionProviderTestGet();
    }

    private Boolean cqlExecutionProviderTestSimpleDate() {
        // execute cql expression on date interval
        Parameters params = parameters(stringPart("expression", "Interval[Today() - 2 years, Today())"));
        Parameters results = runCqlExecution(params);
        return results.getParameter("return").getValue() instanceof Period;
    }

    void cqlExecutionProviderTestSimpleArithmetic() {
        // execute simple cql expression
        Parameters params = parameters(stringPart("expression", "5 * 5"));
        Parameters results = runCqlExecution(params);
        assertInstanceOf(IntegerType.class, results.getParameter("return").getValue());
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }

    void cqlExecutionProviderTestReferencedLibrary() {
        Parameters params = parameters(
                stringPart("subject", "SimplePatient"),
                part(
                        "library",
                        canonicalPart("url", ourClient.getServerBase() + "/Library/SimpleR4Library|0.0.1"),
                        stringPart("name", "SimpleR4Library")),
                stringPart("expression", "SimpleR4Library.simpleBooleanExpression"));

        Parameters results = runCqlExecution(params);
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    void cqlExecutionProviderTestDataBundle() {
        // execute cql expression from library over data from bundle with no subject
        var data = (Bundle) readResource("SimpleDataBundle.json");
        Parameters params = parameters(
                part(
                        "library",
                        canonicalPart("url", ourClient.getServerBase() + "/Library/SimpleR4Library"),
                        stringPart("name", "SimpleR4Library")),
                stringPart("expression", "SimpleR4Library.\"observationRetrieve\""),
                part("data", data),
                booleanPart("useServerData", false));

        Parameters results = runCqlExecution(params);
        assertInstanceOf(Observation.class, results.getParameter().get(0).getResource());
    }

    void cqlExecutionProviderTestDataBundleWithSubject() {
        // execute cql expression from library over data from bundle with subject
        var data = (Bundle) readResource("SimpleDataBundle.json");
        Parameters params = parameters(
                stringPart("subject", "SimplePatient"),
                part(
                        "library",
                        canonicalPart("url", ourClient.getServerBase() + "/Library/SimpleR4Library"),
                        stringPart("name", "SimpleR4Library")),
                stringPart("expression", "SimpleR4Library.\"observationRetrieve\""),
                part("data", data),
                booleanPart("useServerData", false));
        Parameters results = runCqlExecution(params);
        assertInstanceOf(Observation.class, results.getParameter().get(0).getResource());
    }

    void cqlExecutionProviderTestSimpleParameters() {
        // execute inline cql date expression with input value
        Parameters evaluationParams = parameters(datePart("%inputDate", "2019-11-01"));
        Parameters params = parameters(
                stringPart("expression", "year from %inputDate before 2020"), part("parameters", evaluationParams));
        Parameters results = runCqlExecution(params);
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    void cqlExecutionProviderTestExpression() {
        // execute cql expression from referenced library
        Parameters params = parameters(
                stringPart("subject", "Patient/SimplePatient"),
                part(
                        "library",
                        canonicalPart("url", ourClient.getServerBase() + "/Library/SimpleR4Library"),
                        stringPart("name", "SimpleR4Library")),
                stringPart("expression", "SimpleR4Library.\"Numerator\""));

        Parameters results = runCqlExecution(params);

        assertFalse(results.isEmpty());
        assertThat(results.getParameter()).hasSize(1);
        assertInstanceOf(BooleanType.class, results.getParameter("return").getValue());
        assertTrue(((BooleanType) results.getParameter("return").getValue()).booleanValue());
    }

    void cqlExecutionProviderTestErrorExpression() {
        // execute invalid cql expression
        Parameters params = parameters(stringPart("expression", "Interval[1,5]"));

        Parameters results = runCqlExecution(params);

        assertTrue(results.hasParameter());
        assertTrue(results.getParameterFirstRep().hasName());
        assertEquals("return", results.getParameterFirstRep().getName());
        var value = assertInstanceOf(
                StringType.class, results.getParameterFirstRep().getValue());
        assertEquals("Interval[1, 5]", value.toString());
    }

    public Parameters runCqlExecution(Parameters parameters) {

        return ourClient
                .operation()
                .onServer()
                .named(ProviderConstants.CR_OPERATION_CQL)
                .withParameters(parameters)
                .execute();
    }

    void cqlExecutionProviderTestGet() {
        var params = parameters(stringPart("subject", "SimplePatient"), stringPart("expression", "5 * 5"));
        var results = ourClient
                .operation()
                .onServer()
                .named(ProviderConstants.CR_OPERATION_CQL)
                .withParameters(params)
                .useHttpGet()
                .execute();
        assertInstanceOf(IntegerType.class, results.getParameter("return").getValue());
        assertEquals("25", ((IntegerType) results.getParameter("return").getValue()).asStringValue());
    }
}
