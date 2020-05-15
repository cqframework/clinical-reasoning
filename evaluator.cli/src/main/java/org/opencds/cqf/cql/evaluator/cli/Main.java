package org.opencds.cqf.cql.evaluator.cli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.Response;
import org.opencds.cqf.cql.evaluator.builder.BuilderParameters;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderLibraryContext;
import org.opencds.cqf.cql.evaluator.serialization.DefaultEvaluationResultsSerializer;
import org.opencds.cqf.cql.evaluator.serialization.EvaluationResultsSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.opencds.cqf.cql.execution.LibraryResult;

public class Main {

    public static void main(String[] args) {

        // TODO: Update cql engine dependencies
        disableAccessWarnings();

        BuilderParameters params = null;
        try {
            params = new ArgumentProcessor().parseAndConvert(args);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
            Map<Pair<String, String>, String> modelVersionFileUriMap = new HashMap<Pair<String, String>, String>();
            cqlEvaluatorBuilder.withLibraryLoader(params.library)
                .withTerminologyProvider(params.models.get(0).getName(), params.models.get(0).getVersion(), params.terminology)
                .withFileDataProvider(modelVersionFileUriMap)
            Response response  = evaluator.evaluate(params);
            EvaluationResultsSerializer serializer;

            serializer = new DefaultEvaluationResultsSerializer();


            for (Entry<VersionedIdentifier, LibraryResult> libraryEntry : response.evaluationResult.libraryResults.entrySet()) {
                serializer.printResults(params.verbose, libraryEntry);
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
    // 

}