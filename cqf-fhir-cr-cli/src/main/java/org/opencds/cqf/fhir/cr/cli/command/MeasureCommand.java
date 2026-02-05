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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.MeasureCommandArgument;
import org.opencds.cqf.fhir.cr.cli.command.CqlCommand.SubjectAndResult;
import org.opencds.cqf.fhir.cr.cli.command.EngineFactory.EngineBundle;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
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
        var cqlResult = CqlCommand.createCqlCommandResult(args.cql);
        var bundle = cqlResult.engineBundle();

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

        var measureResults = cqlResult
                .subjectResults()
                .map(sr -> evaluateMeasureForSubject(processor, measure, start, end, sr))
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
            SubjectAndResult subjectAndResult) {

        Map<String, EvaluationResult> resultMap = new HashMap<>();
        var subjectId = subjectAndResult.subject().subjectId();
        resultMap.put(subjectId, subjectAndResult.result());

        var report = processor.evaluateMeasureResults(
                measure, start, end, "subject", Collections.singletonList(subjectId), resultMap);
        return new SubjectAndReport(subjectAndResult.subject().value(), report);
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
}
