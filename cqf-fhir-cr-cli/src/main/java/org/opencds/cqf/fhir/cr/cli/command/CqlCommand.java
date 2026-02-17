package org.opencds.cqf.fhir.cr.cli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import kotlin.Unit;
import org.hl7.elm.r1.VersionedIdentifier;
import org.opencds.cqf.cql.engine.execution.EvaluationParams;
import org.opencds.cqf.cql.engine.execution.EvaluationParams.LibraryParams;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cr.cli.argument.CqlCommandArgument;
import org.opencds.cqf.fhir.cr.cli.command.EngineFactory.EngineBundle;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "cql", mixinStandardHelpOptions = true, description = "Evaluate CQL libraries against FHIR resources.")
public class CqlCommand implements Callable<Integer> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CqlCommand.class);

    @ArgGroup(multiplicity = "1", exclusive = false)
    public CqlCommandArgument args;

    @Override
    public Integer call() throws IOException {
        var setupStart = System.nanoTime();
        var result = createCqlCommandResult(this.args);
        var setupEnd = System.nanoTime();
        var initializationTime = (setupEnd - setupStart) / 1_000_000.0; // Convert to milliseconds

        var evalStart = System.nanoTime();

        AtomicLong counter = new AtomicLong();
        result.subjectResults().forEach(x -> {
            counter.incrementAndGet();
        });

        var evalEnd = System.nanoTime();
        var evalTime = (evalEnd - evalStart) / 1_000_000;
        log.info("Completed {} cql evaluations", counter.get());
        log.info("Initialization time: {} ms", initializationTime);
        log.info("Evaluation time: {} ms", evalTime);
        log.info("Average time per evaluation: {} ms", evalTime / counter.get());
        return 0;
    }

    /**
     * Record to hold subject ID and evaluation result.
     * Used by MeasureCommand which needs to collect all results before generating reports.
     */
    public record SubjectAndResult(SubjectContext subject, EvaluationResult result) {
        public String subjectId() {
            return subject.subjectId();
        }
    }

    public record SubjectContext(String name, String value) {
        public String subjectId() {
            return name + "/" + value;
        }
    }

    public record CqlCommandResult(Stream<SubjectAndResult> subjectResults, EngineBundle engineBundle) {}

    /**
     * Evaluates CQL and returns a stream of SubjectAndResult.
     */
    public static CqlCommandResult createCqlCommandResult(CqlCommandArgument arguments) throws IOException {
        Path baseOutput = arguments.outputPath != null ? Path.of(arguments.outputPath) : null;
        if (baseOutput != null) {
            Files.createDirectories(baseOutput);
        }

        VersionedIdentifier identifier = new VersionedIdentifier().withId(arguments.content.name);
        Set<String> expressions = arguments.content.expression != null ? Set.of(arguments.content.expression) : null;
        var bundle = EngineFactory.createEngineBundle(arguments);

        var contexts =
                arguments.parameters.context.stream().map(c -> new SubjectContext(c.contextName, c.contextValue));

        var resultStream = contexts.map(sc -> {
                    var contextParameter = new kotlin.Pair<String, Object>(sc.name(), sc.value());
                    var paramBuilder = new EvaluationParams.Builder();
                    paramBuilder.setContextParameter(contextParameter);
                    if (expressions != null && !expressions.isEmpty()) {
                        paramBuilder.library(identifier, builder -> {
                            builder.expressions(expressions);
                            return Unit.INSTANCE;
                        });
                    } else {
                        paramBuilder.library(identifier, new LibraryParams.Builder().build());
                    }

                    var cqlResult = bundle.engine().evaluate(paramBuilder.build());
                    return new SubjectAndResult(sc, cqlResult.getOnlyResultOrThrow());
                })
                .map(cqlResult -> {
                    if (baseOutput != null) {
                        Path outputPath = baseOutput.resolve(cqlResult.subject().value() + ".txt");
                        try {
                            writeResultToFile(cqlResult.result(), cqlResult.subjectId(), outputPath);
                            log.info("✅ Completed {}", cqlResult.subjectId());
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write CQL results for " + cqlResult.subjectId(), e);
                        }
                    } else {
                        Utilities.writeResult(cqlResult.result(), System.out);
                    }
                    return cqlResult;
                });

        return new CqlCommandResult(resultStream, bundle);
    }

    private static void writeResultToFile(EvaluationResult result, String subjectId, Path outputPath)
            throws IOException {
        try (var writer = Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            for (var entry : result.getExpressionResults().entrySet()) {
                writer.write(entry.getKey() + "="
                        + Utilities.tempConvert(entry.getValue().getValue()));
                writer.newLine();
            }
        }
    }
}
