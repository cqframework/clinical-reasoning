package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.cli.argument.CqlCommandArgument;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "cql", mixinStandardHelpOptions = true, description = "Evaluate CQL libraries against FHIR resources.")
public class CqlCommand implements Callable<Integer> {
    @ArgGroup(multiplicity = "1", exclusive = false)
    public CqlCommandArgument args;

    @Override
    public Integer call() throws IOException {
        var results = evaluate(this.args);

        if (args.outputPath != null) {
            Files.createDirectories(Path.of(args.outputPath));
        }

        OutputStream os = args.outputPath == null
                ? System.out
                : Files.newOutputStream(
                        Path.of(args.outputPath),
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE);

        results.forEach(r -> Utilities.writeResult(r.result(), os));

        if (os != System.out) {
            os.close();
        }

        return 0;
    }

    record SubjectAndResult(String subjectId, EvaluationResult result) {}

    public static Stream<SubjectAndResult> evaluate(CqlCommandArgument arguments) {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(arguments.fhir.fhirVersion));

        var evaluationSettings =
                Utilities.createEvaluationSettings(arguments.content.cqlPath, arguments.hedisCompatibilityMode);

        NpmProcessor npmProcessor = null;
        if (arguments.fhir.implementationGuidePath != null && arguments.fhir.rootDirectory != null) {
            try {
                // LUKETODO:  status quo except stub the NOOP NpmPackageLoader for now
                var context = new IGContext();
                context.initializeFromIg(
                        arguments.fhir.rootDirectory,
                        arguments.fhir.implementationGuidePath,
                        fhirContext.getVersion().getVersion().getFhirVersionString());
                npmProcessor = new NpmProcessor(context);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to initialize IGContext from provided path", e);
            }
        }

        evaluationSettings.setNpmProcessor(npmProcessor);
        var repository = Utilities.createRepository(fhirContext, arguments.fhir.terminologyUrl, arguments.fhir.dataUrl);
        VersionedIdentifier identifier = new VersionedIdentifier().withId(arguments.content.name);

        Set<String> expressions = arguments.content.expression != null ? Set.of(arguments.content.expression) : null;

        return arguments.parameters.context.stream().map(c -> {
            var engine = Engines.forRepository(repository, evaluationSettings);
            if (arguments.content.cqlPath != null) {
                var provider = new DefaultLibrarySourceProvider(Path.of(arguments.content.cqlPath));
                engine.getEnvironment()
                        .getLibraryManager()
                        .getLibrarySourceLoader()
                        .registerProvider(provider);
            }

            // This is incorrect because cql supports multiple context parameters
            // Encounter=123 and Patient=456 can be set simultaneously.
            // For now, we assume only one context parameter is provided.
            var subjectId = c.contextName + "/" + c.contextValue;
            var contextParameter = Pair.<String, Object>of(c.contextName, c.contextValue);
            var cqlResult = engine.evaluate(identifier, expressions, contextParameter);
            return new SubjectAndResult(subjectId, cqlResult);
        });
    }
}
