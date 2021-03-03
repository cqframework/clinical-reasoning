package org.opencds.cqf.cql.evaluator.serialization;

import java.util.Map.Entry;

import com.google.gson.JsonObject;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.exception.CqlException;
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

        if (evaluationResult.getDebugResult() != null) {
            System.out.println("Messages/Warnings/Errors:");
            for (CqlException e : evaluationResult.getDebugResult().getMessages()) {
                System.out.println(String.format(
                        "%s: %s%s",
                        e.getSeverity().toString(),
                        e.getSourceLocator() != null ? (e.getSourceLocator().toString() + ": ") : "",
                        e.getMessage()
                ));
            }
        }

        System.out.println("Results:");
        for (Entry<String, Object> expressionEntry : evaluationResult.expressionResults.entrySet()) {
            this.expressionEntry = expressionEntry;
            System.out.println(
                    String.format("%s = %s", expressionEntry.getKey(), serializeResult()));
        }
    }

    protected String serializeResult(Object result) {
        if (result == null) {
            return null;
        }

        if (result instanceof IBaseResource) {
            return formatResults(getFhirContext().newJsonParser().encodeResourceToString((IBaseResource)result));
        }

        if (result instanceof Iterable) {
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            boolean first = true;
            for (Object o : (Iterable)result) {
                if (first) {
                    first = false;
                }
                else {
                    builder.append(",\n");
                }
                builder.append(formatResults(serializeResult(o)));
            }
            builder.append("\n}");
            return formatResults(builder.toString());
        }

        return formatResults(result.toString());
    }

    protected String serializeResult() {
        return serializeResult(expressionEntry.getValue());
    }

    protected String formatResults(String expressionEntryValue) {
        return expressionEntryValue;
    }

    protected void performRetrieve(Iterable result, JsonObject results) {
        // Perform Retrieve for specific Context
    }

    public FhirContext getFhirContext() {
        if (fhirContext == null) {
            setFhirContext("4.0.1");
        }
        return fhirContext;
    }

    public static void setFhirContext(String version) {
        switch (version) {
            case "2.0.0":
                fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU2_1);
                break;
            case "3.0.0":
                fhirContext = FhirContext.forCached(FhirVersionEnum.DSTU3);
                break;
            case "4.0.0":
            case "4.0.1":
                fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown FHIR data provider version: %s", version));
        }
    }
}