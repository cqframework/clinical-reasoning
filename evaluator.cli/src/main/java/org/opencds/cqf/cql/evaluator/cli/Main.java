package org.opencds.cqf.cql.evaluator.cli;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.cli.temporary.EvaluationParameters;
import org.opencds.cqf.cql.evaluator.dagger.CqlEvaluatorComponent;
import org.opencds.cqf.cql.evaluator.dagger.DaggerCqlEvaluatorComponent;

public class Main {

    public static void main(String[] args) {
        try {
            Main main = new Main();
            main.parseAndExecute(args);

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void parseAndExecute(String[] args) {
        EvaluationParameters parameters = this.parse(args);
        if (parameters == null) {
            return;
        }

        this.execute(parameters);
    }

    public EvaluationParameters parse(String[] args) {
        return new ArgumentProcessor().parseAndConvert(args);
    }


    public void execute(EvaluationParameters parameters) {
        // This is all temporary garbage to get running again.
        // requireNonNull(parameters.contextParameter, "Gotta have a
        // contextParameter.");
        requireNonNull(parameters.libraryName, "Gotta have a libraryName");
        requireNonNull(parameters.libraryUrl, "Gotta have a libraryUrl");
        // requireNonNull(parameters.terminologyUrl, "Gotta have a terminologyUrl");
        // requireNonNull(parameters.model, "Gotta have a model");

        CqlEvaluatorComponent cqlEvaluatorComponent = DaggerCqlEvaluatorComponent.builder().fhirContext(parameters.fhirVersion.newContext()).build();

        CqlEvaluatorBuilder cqlEvaluatorBuilder = cqlEvaluatorComponent.createBuilder();

        LibraryLoader libraryLoader = cqlEvaluatorComponent.createLibraryLoaderFactory()
                .create(new EndpointInfo().setAddress(parameters.libraryUrl), null);

        cqlEvaluatorBuilder.withLibraryLoader(libraryLoader);

        if (parameters.terminologyUrl != null) {
            TerminologyProvider terminologyProvider = cqlEvaluatorComponent.createTerminologyProviderFactory()
                .create(new EndpointInfo().setAddress(parameters.terminologyUrl));

                cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
        }

        Triple<String, ModelResolver, RetrieveProvider> dataProvider = null;
        DataProviderFactory dataProviderFactory = cqlEvaluatorComponent.createDataProviderFactory();
        if (parameters.model != null) {
            dataProvider = dataProviderFactory
                .create(new EndpointInfo().setAddress(parameters.model.getRight()));
        }
        // default to FHIR
        else {
            dataProvider = dataProviderFactory
            .create(new EndpointInfo().setType(Constants.HL7_FHIR_FILES_CODE));
        }

        cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider.getLeft(), dataProvider.getMiddle(),
        dataProvider.getRight());


        CqlEvaluator evaluator = cqlEvaluatorBuilder.build();

        VersionedIdentifier identifier = new VersionedIdentifier().withId(parameters.libraryName);

        Pair<String, Object> contextParameter = null;
        
        if (parameters.contextParameter != null)
        {
            contextParameter = Pair.of(parameters.contextParameter.getLeft(),
            parameters.contextParameter.getRight());
        }

        EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

        for (Map.Entry<String, Object> libraryEntry : result.expressionResults.entrySet()) {
            System.out.println(libraryEntry.getKey() + "="
                    + (libraryEntry.getValue() != null ? libraryEntry.getValue().toString() : null));
        }
    }
}