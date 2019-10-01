package com.alphora.cql.cli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map.Entry;

import com.alphora.cql.service.ServiceParameters;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.execution.EvaluationResult;
import org.opencds.cqf.cql.execution.LibraryResult;

public class Main {

    public static void main(String[] args) {

        // TODO: Update cql engine dependencies
        disableAccessWarnings();

        ServiceParameters params = null;
        try {
            params = ArgumentProcessor.parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            EvaluationResult result = com.alphora.cql.service.Service.evaluate(params);

            for (Entry<VersionedIdentifier, LibraryResult> libraryEntry : result.libraryResults.entrySet()) {
                for (Entry<String, Object> expressionEntry : libraryEntry.getValue().expressionResults.entrySet()) {
                    System.out.println(String.format("%s.%s = %s", libraryEntry.getKey().getId(), expressionEntry.getKey(), expressionEntry.getValue()));
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