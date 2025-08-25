package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
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
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.MeasureCommandArgument;
import org.opencds.cqf.fhir.cr.cli.command.CqlCommand.SubjectAndResult;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.npm.R4FhirOrNpmResourceProvider;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
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
        var results = MeasureCommand.evaluate(this.args);

        if (args.cql.outputPath != null) {
            Files.createDirectories(Path.of(args.cql.outputPath));
        }

        var fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(args.cql.fhir.fhirVersion));
        var parser = fhirContext.newJsonParser();
        results.forEach(r -> {
            var json = parser.encodeResourceToString(r.measureReport);
            if (args.reportPath != null) {
                writeJsonToFile(json, r.subjectId, Path.of(args.reportPath));
            } else {
                System.out.println(json);
            }

            if (args.cql.outputPath != null) {
                try {
                    var path = Path.of(args.cql.outputPath, r.subjectId + ".txt");
                    Files.createDirectories(path.getParent());
                    OutputStream out = Files.newOutputStream(
                            path,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                    Utilities.writeResult(r.result, out);
                    out.close();
                    log.info("Cql for patient {} written to: {}", r.subjectId, path);
                } catch (IOException e) {
                    log.error("Failed to write cql for {}", r.subjectId, e);
                }
            }
        });

        return 0; // Return an appropriate exit code
    }

    record SubjectAndReport(String subjectId, EvaluationResult result, MeasureReport measureReport) {}

    public static Stream<SubjectAndReport> evaluate(MeasureCommandArgument args) {
        var cqlArgs = args.cql;
        var results = CqlCommand.evaluate(cqlArgs);
        var fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(cqlArgs.fhir.fhirVersion));
        var parser = fhirContext.newJsonParser();
        var resource = getMeasure(parser, args.measurePath, args.measureName);
        var processor = getR4MeasureProcessor(
                Utilities.createEvaluationSettings(cqlArgs.content.cqlPath, cqlArgs.hedisCompatibilityMode),
                Utilities.createRepository(fhirContext, cqlArgs.fhir.terminologyUrl, cqlArgs.fhir.dataUrl),
                Utilities.createNpmPackageLoader());

        var start = args.periodStart != null
                ? LocalDate.parse(args.periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay(ZoneId.systemDefault())
                : null;

        var end = args.periodEnd != null
                ? LocalDate.parse(args.periodEnd, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atTime(LocalTime.MAX)
                        .atZone(ZoneId.systemDefault())
                : null;

        // Something askew here, we should get one result per subject for subject report.
        // Probably need to refactor the evaluateMeasureResults method to handle this.
        // "subject" and "summary" need separate overloads, possibly with different parameter types.
        var map = results.collect(Collectors.toMap(SubjectAndResult::subjectId, SubjectAndResult::result));

        return map.entrySet().stream().map(entry -> {
            var report = processor.evaluateMeasureResults(
                    resource, start, end, "subject", Collections.singletonList(entry.getKey()), map);
            return new SubjectAndReport(entry.getKey(), entry.getValue(), report);
        });
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

    @Nonnull
    private static R4MeasureProcessor getR4MeasureProcessor(
            EvaluationSettings evaluationSettings, IRepository repository, NpmPackageLoader npmPackageLoader) {

        MeasureEvaluationOptions evaluationOptions = new MeasureEvaluationOptions();
        evaluationOptions.setApplyScoringSetMembership(false);
        evaluationOptions.setEvaluationSettings(evaluationSettings);

        return new R4MeasureProcessor(
                repository,
                evaluationOptions,
                new MeasureProcessorUtils(),
                getR4FhirOrNpmResourceLoader(repository, npmPackageLoader, evaluationOptions));
    }

    private static R4FhirOrNpmResourceProvider getR4FhirOrNpmResourceLoader(
            IRepository repository, NpmPackageLoader npmPackageLoader, MeasureEvaluationOptions evaluationOptions) {
        return new R4FhirOrNpmResourceProvider(repository, npmPackageLoader, evaluationOptions);
    }

    private void writeJsonToFile(String json, String patientId, Path path) {
        Path outputPath = path.resolve(patientId + ".json");

        try {
            // Ensure parent directories exist
            Files.createDirectories(outputPath.getParent());

            // Write JSON to file
            try (OutputStream out = Files.newOutputStream(
                    outputPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                out.write(json.getBytes());
                log.info("report for patient {} written to: {}", patientId, outputPath);
            }

        } catch (IOException e) {
            log.error("Failed to write JSON for patient {}", patientId, e);
        }
    }
}
