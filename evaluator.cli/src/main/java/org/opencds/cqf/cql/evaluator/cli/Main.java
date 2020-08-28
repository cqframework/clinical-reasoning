package org.opencds.cqf.cql.evaluator.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.inject.Guice;
import com.google.inject.Injector;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.api.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.api.ParameterParser;
import org.opencds.cqf.cql.evaluator.builder.api.DataProviderConfigurer;
import org.opencds.cqf.cql.evaluator.builder.api.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.api.LibraryLoaderFactory;
import org.opencds.cqf.cql.evaluator.builder.api.TerminologyProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.api.model.DataProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.di.EvaluatorModule;
import org.opencds.cqf.cql.evaluator.builder.di.FhirContextModule;
import org.opencds.cqf.cql.evaluator.cli.temporary.EvaluationParameters;

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
            new FhirContextModule(fhirVersionEnum),
            new EvaluatorModule());
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

        Pair<String, DataProvider> dataProvider = this.get(DataProviderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.model != null ? parameters.model.getRight() : null));

        this.get(DataProviderConfigurer.class)
                .configure(dataProvider.getRight(), new DataProviderConfig().setTerminologyProvider(terminologyProvider));

        Map<String,DataProvider> dataProviders = new HashMap<String,DataProvider>();
        dataProviders.put(dataProvider.getLeft(), dataProvider.getRight());

        CqlEvaluator evaluator = new org.opencds.cqf.cql.evaluator.CqlEvaluator(libraryLoader, dataProviders,
                terminologyProvider);

        VersionedIdentifier identifier = new VersionedIdentifier().withId(parameters.libraryName);

        ParameterParser parser = this.get(ParameterParser.class);

        Pair<String, Object> contextParameter = parser.parseContextParameter(libraryLoader, identifier, parameters.contextParameter);
        EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

        for (Map.Entry<String, Object> libraryEntry : result.expressionResults.entrySet()) {
            System.out.println(libraryEntry.getKey() + "=" + (libraryEntry.getValue() != null ? libraryEntry.getValue().toString() : null));
        }
    }
}
