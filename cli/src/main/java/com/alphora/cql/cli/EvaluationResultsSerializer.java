package com.alphora.cql.cli;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.*;

import ca.uhn.fhir.context.FhirContext;
import org.opencds.cqf.cql.retrieve.FhirBundleCursor;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class EvaluationResultsSerializer {
    public EvaluationResultsSerializer () {
        //set Version and Model
        //later on this needs to be abstracted and based on the model and version serialize accordingly
    }
    public String serializeResult(Object expressionEntry) {
        JsonObject result = new JsonObject();
        if (expressionEntry == null) {
            result.add("result", new JsonPrimitive("Null"));
        } 
        else if (expressionEntry instanceof FhirBundleCursor) 
        {
            performRetrieve((Iterable) expressionEntry, result);
        }
        else if (expressionEntry instanceof List)
        {
            if (((List) expressionEntry).size() > 0 && ((List) expressionEntry).get(0) instanceof IBaseResource)
            {
                performRetrieve((Iterable) expressionEntry, result);
            }
            else
            {
                result.add("result", new JsonPrimitive(expressionEntry.toString()));
            }
        }
        else if (expressionEntry instanceof IBaseResource)
        {
            result.add("result", new JsonPrimitive(FhirContext.forDstu3().newJsonParser().setPrettyPrint(true).encodeResourceToString((IBaseResource) expressionEntry)));
        }
        else
        {
            result.add("result", new JsonPrimitive(expressionEntry.toString()));
        }
        result.add("resultType", new JsonPrimitive(resolveType(expressionEntry)));

        return result.toString();

    }
    private void performRetrieve(Iterable result, JsonObject results) {
        //should pass in any context
        FhirContext fhirContext = FhirContext.forDstu3(); // for JSON parsing
        Iterator it = result.iterator();
        List<Object> findings = new ArrayList<>();

        while (it.hasNext()) {
            // returning full JSON retrieve response
            findings.add(fhirContext
                    .newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString((org.hl7.fhir.instance.model.api.IBaseResource)it.next()));
        }

        results.add("result", new JsonPrimitive(findings.toString()));
    }

    private String resolveType(Object result) {
        String type = result == null ? "Null" : result.getClass().getSimpleName();
        switch (type) {
            case "BigDecimal": return "Decimal";
            case "ArrayList": return "List";
            case "FhirBundleCursor": return "Retrieve";
        }
        return type;
    }
}