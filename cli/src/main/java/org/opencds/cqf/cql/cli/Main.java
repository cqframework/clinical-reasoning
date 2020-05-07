package org.opencds.cqf.cql.cli;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map.Entry;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.service.Parameters;
import org.opencds.cqf.cql.service.Response;
import org.opencds.cqf.cql.service.Service;
import org.opencds.cqf.cql.service.serialization.DefaultEvaluationResultsSerializer;
// import org.opencds.cqf.cql.service.serialization.DefaultEvaluationResultsSerializer;
// import org.opencds.cqf.cql.service.serialization.EvaluationResultsSerializer;
import org.opencds.cqf.cql.service.serialization.EvaluationResultsSerializer;

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
            Service service = new Service(EnumSet.of(Service.Options.EnableFileUri));
            Response response = service.evaluate(params);
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