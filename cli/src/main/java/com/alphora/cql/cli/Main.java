package com.alphora.cql.cli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map.Entry;

import com.alphora.cql.service.Parameters;
import com.alphora.cql.service.Response;
import com.alphora.cql.service.Service;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.execution.LibraryResult;

public class Main {

    public static void main(String[] args) {

        // TODO: Update cql engine dependencies
        disableAccessWarnings();

        Parameters params = null;
        try {
            params = new ArgumentProcessor().parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            Service service = new Service(EnumSet.of(Service.Options.EnableFileUri));
            Response response  = service.evaluate(params);
            EvaluationResultsSerializer serializer = new EvaluationResultsSerializer();

            for (Entry<VersionedIdentifier, LibraryResult> libraryEntry : response.evaluationResult.libraryResults.entrySet()) {
                for (Entry<String, Object> expressionEntry : libraryEntry.getValue().expressionResults.entrySet()) {
                    if (!params.verbose)
                    {
                        String serializedExpressionEntry = serializer.serializeResult(expressionEntry.getValue());
                        String objectString = (expressionEntry.getValue() == null) ? null : expressionEntry.getValue().toString();
                        int idStartingIndex = serializedExpressionEntry.indexOf("id");
                        int idEndingIndex = serializedExpressionEntry.indexOf("\\n", idStartingIndex);
                        String expressionEntryValue;

                        if (idStartingIndex != -1 && idEndingIndex != -1) {
                            int objectStringIdIndex = (objectString == null) ? -1 : objectString.indexOf("@");
                            if(objectStringIdIndex != -1) {
                                objectString = objectString.substring(0, objectStringIdIndex) + "_"; 
                            }
                            expressionEntryValue = objectString
                                + serializedExpressionEntry.substring(idStartingIndex, idEndingIndex).replace("\\", "").replace("\"", "");
                        }
                        else expressionEntryValue = objectString;

                        System.out.println(String.format("%s.%s = %s", libraryEntry.getKey().getId(), expressionEntry.getKey(), expressionEntryValue));
                    }
                    else {
                        String lineSeperator = System.getProperty("line.separator");
                        String expressionEntryValue = serializer.serializeResult(expressionEntry.getValue()).replace("\\n", lineSeperator).replace("\\", "");
                        System.out.println(String.format("%s.%s = %s", libraryEntry.getKey().getId(), expressionEntry.getKey(), expressionEntryValue));
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);

            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }

}