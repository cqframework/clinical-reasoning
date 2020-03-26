package org.opencds.cqf.cql.cli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map.Entry;

import org.opencds.cqf.cql.evaluator.Evaluator;
import org.opencds.cqf.cql.evaluator.Parameters;
import org.opencds.cqf.cql.evaluator.Response;
import org.opencds.cqf.cql.evaluator.serialization.DefaultEvaluationResultsSerializer;
import org.opencds.cqf.cql.evaluator.serialization.EvaluationResultsSerializer;

public class Main {

    public static void main(String[] args) {
        Parameters params = null;
        try {
            params = new ArgumentProcessor().parseAndConvert(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            Evaluator evaluator = new Evaluator(EnumSet.of(Evaluator.Options.EnableFileUri));
            Response response  = evaluator.evaluate(params);
            EvaluationResultsSerializer serializer;

            serializer = new DefaultEvaluationResultsSerializer();

            serializer.printResults(params.verbose, response.evaluationResult);
        } catch (

        Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}