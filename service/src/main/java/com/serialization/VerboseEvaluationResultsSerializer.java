package com.serialization;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.*;

import org.opencds.cqf.cql.execution.LibraryResult;
import org.opencds.cqf.cql.retrieve.FhirBundleCursor;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseResource;
import java.util.Map.Entry;

public class VerboseEvaluationResultsSerializer extends EvaluationResultsSerializer{

    private final char[] unwantedCharacters = { '\\',  '\"' };
    private boolean verbose;
    private String libraryEntryKeyId;
    private Entry<String, Object> expressionEntry;
    private Object expressionEntryObject;

    @Override
    public void printResults(boolean verbose, Entry<VersionedIdentifier, LibraryResult> libraryEntry) {

        this.verbose = verbose;
        
        for (Entry<String, Object> expressionEntry : libraryEntry.getValue().expressionResults.entrySet()) {

            this.expressionEntry = expressionEntry;
            this.expressionEntryObject = expressionEntry.getValue();
            this.libraryEntryKeyId = libraryEntry.getKey().getId();

            String serializedExpressionEntryObject = this.serializeResult();
            System.out.println(formatResults(serializedExpressionEntryObject));
        }
    }

    @Override
    protected String serializeResult() {
        if (verbose) { return serializeResultVerbose(); }
        else return serializeResultNonVerbose();

    }

    private String serializeResultVerbose() {
        JsonObject result = new JsonObject();
        if (expressionEntryObject == null) {
            result.add("result", new JsonPrimitive("Null"));
        } 
        else if (expressionEntryObject instanceof FhirBundleCursor) 
        {
            performRetrieve((Iterable) expressionEntryObject, result);
        }
        else if (expressionEntryObject instanceof List)
        {
            if (((List) expressionEntryObject).size() > 0 && ((List) expressionEntryObject).get(0) instanceof IBaseResource)
            {
                performRetrieve((Iterable) expressionEntryObject, result);
            }
            else
            {
                result.add("result", new JsonPrimitive(expressionEntryObject.toString()));
            }
        }
        else if (expressionEntryObject instanceof IBaseResource)
        {
            result.add("result", new JsonPrimitive(this.getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString((IBaseResource) expressionEntryObject)));
        }
        else if (expressionEntryObject instanceof org.cqframework.cql.elm.execution.FunctionDef) 
        {
            result.add("result", new JsonPrimitive("Definition successfully validated"));
        }
        else
        {
            result.add("result", new JsonPrimitive(expressionEntryObject.toString()));
        }
        result.add("resultType", new JsonPrimitive(resolveType(expressionEntryObject)));

        return result.toString();
    }

    private String serializeResultNonVerbose() {
        String serializedExpressionEntryObject = this.serializeResultVerbose();
        return addIdOfObjectToResult(serializedExpressionEntryObject);
    }

    private String addIdOfObjectToResult(String serializedExpressionEntry) {
        String objectString = (expressionEntryObject == null) ? null : expressionEntryObject.toString();
        int idStartingIndex = serializedExpressionEntry.indexOf("id");
        int idEndingIndex = serializedExpressionEntry.indexOf("\\n", idStartingIndex);

        if (idStartingIndex != -1 && idEndingIndex != -1) {
            int objectStringIdIndex = (objectString == null) ? -1 : objectString.indexOf("@");
            if(objectStringIdIndex != -1) {
                objectString = objectString.substring(0, objectStringIdIndex) + "_"; 
            }
            return objectString + serializedExpressionEntry.substring(idStartingIndex, idEndingIndex);
        }
        else return objectString;
    }

    @Override
    protected String formatResults(String serializedResultString) {
        if(verbose) {
            String lineSeperator = System.getProperty("line.separator");
            String cleanedUpResult = removeUnwantedCharacters(serializedResultString.replace("\\n", lineSeperator), unwantedCharacters);
            return String.format("%s.%s = %s", libraryEntryKeyId, expressionEntry.getKey(), cleanedUpResult);
        }
        else {
            String cleanedUpResult = removeUnwantedCharacters(addIdOfObjectToResult(serializedResultString), unwantedCharacters);
            return String.format("%s.%s = %s", libraryEntryKeyId, expressionEntry.getKey(), cleanedUpResult);
        }
    }

    private String removeUnwantedCharacters(String string, char[] chars) {
        for (char character : chars) {
            string.replace(character, '\0');
        }
        return string;
    }

    @Override
    protected void performRetrieve(Iterable result, JsonObject results) {
        Iterator it = result.iterator();
        List<Object> findings = new ArrayList<>();

        while (it.hasNext()) {
            // returning full JSON retrieve response
            findings.add(this.getFhirContext()
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