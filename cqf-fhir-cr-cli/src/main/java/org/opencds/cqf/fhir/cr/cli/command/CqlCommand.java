package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.cli.argument.CqlArgument;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "cql", mixinStandardHelpOptions = true)
public class CqlCommand implements Callable<Integer> {
    @ArgGroup(multiplicity = "1..1", exclusive = false)
    public CqlArgument cql;

    public static Map<String, EvaluationResult> evaluate(CqlArgument arguments) {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(arguments.fhir.fhirVersion));
        var context = new IGContext();
        // TODO:
        // context.initializeFromIg(null, null, null);
        var npmProcessor = new NpmProcessor(context);
        var evaluationSettings = Utilities.createEvaluationSettings(arguments.library.libraryUrl, arguments.runtime);
        evaluationSettings.setNpmProcessor(npmProcessor);

        var repository = Utilities.createRepository(fhirContext, arguments.terminologyUrl, arguments.model.modelUrl);
        VersionedIdentifier identifier = new VersionedIdentifier().withId(arguments.library.libraryName);

        var results = new HashMap<String, EvaluationResult>();
        for (var e : arguments.evaluation) {
            var engine = Engines.forRepository(repository, evaluationSettings);

            if (arguments.library.libraryUrl != null) {
                var provider = new DefaultLibrarySourceProvider(Path.of(arguments.library.libraryUrl));
                engine.getEnvironment()
                        .getLibraryManager()
                        .getLibrarySourceLoader()
                        .registerProvider(provider);
            }

            // This is incorrect because cql supports multiple context parameters
            // Encounter=123 and Patient=456 can be set simultaneously.
            // For now, we assume only one context parameter is provided.
            var contextParameter = Pair.<String, Object>of(e.context.contextName, e.context.contextValue);
            var cqlResult = engine.evaluate(identifier, contextParameter);

            var subjectId = e.context.contextName + "/" + e.context.contextValue;
            results.put(subjectId, cqlResult);
        }

        return results;
    }

    @Override
    public Integer call() throws Exception {
        var results = evaluate(this.cql);

        OutputStream os = System.out;
        if (cql.outputPath != null) {
            os = Files.newOutputStream(Path.of(cql.outputPath));
        }

        for (var r : results.values()) {
            Utilities.writeResult(r, os);
        }

        return 0;
    }
}
