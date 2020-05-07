package org.opencds.cqf.cql.evaluator.serialization;

import java.util.Map.Entry;

import com.google.gson.JsonObject;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;

import ca.uhn.fhir.context.FhirContext;

public abstract class EvaluationResultsSerializer {
    private static FhirContext fhirContext;

    private Entry<String, Object> expressionEntry;

    // add a deserializer
    protected IBaseResource deserializeResult() {
        // should return the resource that the json or represents
        return new Bundle();
    }

    public void printResults(Boolean verbose, EvaluationResult evaluationResult) {
        for (Entry<String, Object> expressionEntry : evaluationResult.expressionResults.entrySet()) {
            this.expressionEntry = expressionEntry;
            System.out.println(
                    String.format("%s = %s", expressionEntry.getKey(), expressionEntry.getKey(), serializeResult()));
        }
    }

    protected String serializeResult() {
        return (expressionEntry.getValue() == null) ? null : formatResults(expressionEntry.getValue().toString());
    }

    protected String formatResults(String expressionEntryValue) {
        return expressionEntryValue;
    }

    protected void performRetrieve(Iterable result, JsonObject results) {
        // Perform Retrieve for specific Context
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    public static void setFhirContext(String version) {
        switch (version) {
            case "2.0.0":
                fhirContext = FhirContext.forDstu2_1();
                break;
            case "3.0.0":
                fhirContext = FhirContext.forDstu3();
                break;
            case "4.0.0":
                fhirContext = FhirContext.forR4();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR data provider version: %s", version));
        }
    }
}