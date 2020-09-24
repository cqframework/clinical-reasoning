package org.opencds.cqf.cql.evaluator.cli;

import java.util.Map;
import java.util.Objects;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.cli.temporary.EvaluationParameters;
import org.opencds.cqf.cql.evaluator.guice.builder.BuilderModule;
import org.opencds.cqf.cql.evaluator.guice.fhir.FhirModule;

import ca.uhn.fhir.context.FhirVersionEnum;

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

    protected Injector injector = null;

    protected void initialize(FhirVersionEnum fhirVersionEnum) {
        this.injector = Guice.createInjector(
            new FhirModule(fhirVersionEnum),
            new BuilderModule());
    }

    protected <T> T get(Class<T> clazz) {
        return this.injector.getInstance(clazz);
    }

    public void execute(EvaluationParameters parameters){
        // This is all temporary garbage to get running again.
        //Objects.requireNonNull(parameters.contextParameter, "Gotta have a contextParameter.");
        Objects.requireNonNull(parameters.libraryName, "Gotta have a libraryName");
        Objects.requireNonNull(parameters.libraryUrl, "Gotta have a libraryUrl");
        // Objects.requireNonNull(parameters.terminologyUrl, "Gotta have a terminologyUrl");
        // Objects.requireNonNull(parameters.model, "Gotta have a model");

        this.initialize(parameters.fhirVersion);

        LibraryLoader libraryLoader = this.get(LibraryLoaderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.libraryUrl), null);

        TerminologyProvider terminologyProvider = this.get(TerminologyProviderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.terminologyUrl));

        Triple<String, ModelResolver, RetrieveProvider> dataProvider = this.get(DataProviderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.model != null ? parameters.model.getRight() : null));

        CqlEvaluatorBuilder cqlEvaluatorBuilder = this.get(CqlEvaluatorBuilder.class);

        cqlEvaluatorBuilder.withLibraryLoader(libraryLoader)
            .withTerminologyProvider(terminologyProvider)
            .withModelResolverAndRetrieveProvider(dataProvider.getLeft(), dataProvider.getMiddle(), dataProvider.getRight());

        CqlEvaluator evaluator = cqlEvaluatorBuilder.build();

        VersionedIdentifier identifier = new VersionedIdentifier().withId(parameters.libraryName);

        Pair<String, Object> contextParameter = Pair.of(parameters.contextParameter.getLeft(), parameters.contextParameter.getRight());

        EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

        for (Map.Entry<String, Object> libraryEntry : result.expressionResults.entrySet()) {
            System.out.println(libraryEntry.getKey() + "=" + (libraryEntry.getValue() != null ? libraryEntry.getValue().toString() : null));
        }
    }
}
