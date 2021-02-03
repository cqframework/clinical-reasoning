package org.opencds.cqf.cql.evaluator.cli;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

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
import org.opencds.cqf.cql.evaluator.builder.Constants;
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
        disableAccessWarnings();

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

    public void execute(EvaluationParameters parameters) {
        // This is all temporary garbage to get running again.
        // requireNonNull(parameters.contextParameter, "Gotta have a
        // contextParameter.");
        requireNonNull(parameters.libraryName, "Gotta have a libraryName");
        requireNonNull(parameters.libraryUrl, "Gotta have a libraryUrl");
        // requireNonNull(parameters.terminologyUrl, "Gotta have a terminologyUrl");
        // requireNonNull(parameters.model, "Gotta have a model");

        this.initialize(parameters.fhirVersion);


        CqlEvaluatorBuilder cqlEvaluatorBuilder = this.get(CqlEvaluatorBuilder.class);

        LibraryLoader libraryLoader = this.get(LibraryLoaderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.libraryUrl), null);

        cqlEvaluatorBuilder.withLibraryLoader(libraryLoader);

        if (parameters.terminologyUrl != null) {
            TerminologyProvider terminologyProvider = this.get(TerminologyProviderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.terminologyUrl));

                cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
        }

        Triple<String, ModelResolver, RetrieveProvider> dataProvider = null;
        if (parameters.model != null) {
            dataProvider = this.get(DataProviderFactory.class)
                .create(new EndpointInfo().setAddress(parameters.model.getRight()));
        }
        // default to FHIR
        else {
            dataProvider = this.get(DataProviderFactory.class)
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

    // TODO: Fix when next version of guice is released.
    // Or use Spring instead
    public static void disableAccessWarnings() {
        try {
           Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
           Field field = unsafeClass.getDeclaredField("theUnsafe");
           field.setAccessible(true);
           Object unsafe = field.get(null);
  
           Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
           Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
  
           Class<?> loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
           Field loggerField = loggerClass.getDeclaredField("logger");
           Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
           putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception ignored) {
        }
    }
}
