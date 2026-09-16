package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.MeasureCommandArgument;
import org.opencds.cqf.fhir.cr.cli.command.CqlCommand.SubjectContext;
import org.opencds.cqf.fhir.cr.cli.command.EngineFactory.EngineBundle;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.CompositeEvaluationResultsPerMeasure;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(
        name = "measure",
        mixinStandardHelpOptions = true,
        description = "Evaluate FHIR Measures against FHIR resources.")
public class MeasureCommand implements Callable<Integer> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MeasureCommand.class);

    @ArgGroup(multiplicity = "1", exclusive = false)
    public MeasureCommandArgument args;

    @Override
    public Integer call() throws IOException {
        var setupStart = System.nanoTime();
        var result = createMeasureCommandResult(this.args);
        var setupEnd = System.nanoTime();
        var initializationTime = (setupEnd - setupStart) / 1_000_000.0; // Convert to milliseconds

        var evalStart = System.nanoTime();

        AtomicLong counter = new AtomicLong();
        result.measureReports().forEach(x -> {
            counter.incrementAndGet();
        });

        var evalEnd = System.nanoTime();
        var evalTime = (evalEnd - evalStart) / 1_000_000;
        log.info("Completed evaluation for {} measure reports", counter.get());
        log.info("Initialization time: {} ms", initializationTime);
        log.info("Evaluation time: {} ms", evalTime);
        log.info("Average time per measure report: {} ms", evalTime / counter.get());
        return 0;
    }

    public record MeasureCommandResult(Stream<MeasureReport> measureReports, EngineBundle engineBundle) {}

    public static MeasureCommandResult createMeasureCommandResult(MeasureCommandArgument args) throws IOException {
        // Build the engine bundle directly (rather than via CqlCommand's incomplete library-only
        // $evaluate) so that CR owns the full measure evaluation. This routes the CLI through the
        // same engine path used by the server/R4CollectDataService, which runs the stratifier
        // function evaluation (FunctionEvaluationHandler.cqlFunctionEvaluation) that the
        // pre-calculated `evaluateMeasureResults` path never invokes.
        var bundle = EngineFactory.createEngineBundle(args.cql);
        var engine = bundle.engine();

        var measure = bundle.repository().read(Measure.class, new IdType(args.measureName));

        // Create measure processor once
        R4MeasureProcessor processor = getR4MeasureProcessor(
                bundle.evaluationSettings(), bundle.repository(), Boolean.parseBoolean(args.applyScoring));

        // Parse period dates once
        var start = args.periodStart != null
                ? LocalDate.parse(args.periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay(ZoneId.systemDefault())
                : null;

        var end = args.periodEnd != null
                ? LocalDate.parse(args.periodEnd, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atTime(LocalTime.MAX)
                        .atZone(ZoneId.systemDefault())
                : null;

        Path reportOutput = args.reportPath != null ? Path.of(args.reportPath) : null;
        if (reportOutput != null) {
            Files.createDirectories(reportOutput);
        }

        // Preserve the --output-path per-subject CQL expression dump. This used to be produced by
        // CqlCommand; now we write it from the engine-path results instead.
        Path txtOutput = args.cql.outputPath != null ? Path.of(args.cql.outputPath) : null;
        if (txtOutput != null) {
            Files.createDirectories(txtOutput);
        }

        // Subjects come from the -c/-cv context pairs, same source CqlCommand uses.
        var subjectContexts =
                args.cql.parameters.context.stream().map(c -> new SubjectContext(c.contextName, c.contextValue));

        var measureResults = subjectContexts
                .map(sc -> evaluateMeasureForSubject(processor, measure, start, end, engine, sc, txtOutput))
                .map(sr -> {
                    if (reportOutput != null) {
                        var json = bundle.parser().encodeResourceToString(sr.report());
                        try {
                            writeMeasureReportToFile(json, sr.subjectId(), reportOutput);
                            log.info("Measure report for {} written to: {}", sr.subjectId(), reportOutput);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to write measure report for " + sr.subjectId(), e);
                        }
                    } else {
                        System.out.println(bundle.parser().encodeResourceToString(sr.report()));
                    }

                    log.info("✅ Completed {}", sr.subjectId());
                    return sr.report();
                });

        return new MeasureCommandResult(measureResults, bundle);
    }

    @Nullable
    private static Measure getMeasure(IParser parser, String measurePath, String measureName) {
        if (measureName == null || measurePath == null) {
            return null;
        }

        try (var is = Files.newInputStream(Path.of(measurePath, measureName + ".json"))) {
            return (Measure) parser.parseResource(is);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "measureName: %s not found at path: %s".formatted(measureName, measurePath), e);
        }
    }

    public record SubjectAndReport(String subjectId, MeasureReport report) {}

    private static SubjectAndReport evaluateMeasureForSubject(
            R4MeasureProcessor processor,
            Measure measure,
            ZonedDateTime start,
            ZonedDateTime end,
            CqlEngine engine,
            SubjectContext subjectContext,
            Path txtOutput) {

        // Full subject id (e.g. "Patient/example"), matching the key used in the engine-path results.
        var subjectId = subjectContext.subjectId();
        var subjects = Collections.singletonList(subjectId);

        // Let CR compute the CQL AND the stratifier functions (engine path), exactly like the
        // server/R4CollectDataService does.
        var composite = processor.evaluateMeasureWithCqlEngine(subjects, measure, start, end, new Parameters(), engine);

        var report = processor.evaluateMeasure(
                measure, start, end, "subject", subjects, MeasureEvalType.SUBJECT, engine, composite);

        if (txtOutput != null) {
            writeCqlResultsToFile(composite, subjectId, subjectContext.value(), txtOutput);
        }

        return new SubjectAndReport(subjectContext.value(), report);
    }

    @Nonnull
    private static R4MeasureProcessor getR4MeasureProcessor(
            EvaluationSettings evaluationSettings, IRepository repository, boolean applyScoring) {

        MeasureEvaluationOptions evaluationOptions = new MeasureEvaluationOptions();
        evaluationOptions.setApplyScoringSetMembership(applyScoring);
        evaluationOptions.setEvaluationSettings(evaluationSettings);

        return new R4MeasureProcessor(repository, evaluationOptions);
    }

    private static void writeMeasureReportToFile(String json, String contextValue, Path path) throws IOException {
        Path outputPath = path.resolve(contextValue + ".json");
        // Ensure parent directories exist
        Files.createDirectories(outputPath.getParent());

        // Write JSON to file
        try (OutputStream out = Files.newOutputStream(
                outputPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            out.write(json.getBytes());
        }
    }

    /**
     * Writes the per-subject CQL expression results for the (single) evaluated measure to a
     * {@code <output-path>/<contextValue>.txt} file. The results come from the engine-path
     * composite, so in addition to the library's top-level expression results they may now also
     * include the per-resource stratifier-function results.
     */
    private static void writeCqlResultsToFile(
            CompositeEvaluationResultsPerMeasure composite, String subjectId, String contextValue, Path txtOutput) {
        Path outputPath = txtOutput.resolve(contextValue + ".txt");
        try (OutputStream out = Files.newOutputStream(
                outputPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (var subjectResults : composite.getResultsPerMeasure().values()) {
                var cqlResult = subjectResults.get(subjectId);
                if (cqlResult != null) {
                    Utilities.writeResult(cqlResult.getResult(), out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CQL results for " + subjectId, e);
        }
    }
}
