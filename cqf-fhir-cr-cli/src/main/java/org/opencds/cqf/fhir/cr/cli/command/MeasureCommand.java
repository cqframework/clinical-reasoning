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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.MeasureCommandArgument;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.common.MeasureProcessorUtils;
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
        evaluateAndWriteResults(this.args);
        return 0;
    }

    public static void evaluateAndWriteResults(MeasureCommandArgument args) throws IOException {
        var cqlArgs = args.cql;

        // Set up FhirContext once
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(cqlArgs.fhir.fhirVersion));
        IParser parser = fhirContext.newJsonParser();

        // Set up evaluation settings once
        var evaluationSettings =
                Utilities.createEvaluationSettings(cqlArgs.content.cqlPath, cqlArgs.hedisCompatibilityMode);

        // Initialize NPM processor once if needed
        NpmProcessor npmProcessor = null;
        if (cqlArgs.fhir.implementationGuidePath != null && cqlArgs.fhir.rootDirectory != null) {
            try {
                var context = new IGContext();
                context.initializeFromIg(
                        cqlArgs.fhir.rootDirectory,
                        cqlArgs.fhir.implementationGuidePath,
                        fhirContext.getVersion().getVersion().getFhirVersionString());
                npmProcessor = new NpmProcessor(context);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to initialize IGContext from provided path", e);
            }
        }

        evaluationSettings.setNpmProcessor(npmProcessor);

        // Create repository once
        var repository = Utilities.createRepository(fhirContext, cqlArgs.fhir.terminologyUrl, cqlArgs.fhir.dataUrl);

        // Create engine once
        var engine = Engines.forRepository(repository, evaluationSettings);

        // Register library source provider once
        if (cqlArgs.content.cqlPath != null) {
            var provider = new DefaultLibrarySourceProvider(
                    new kotlinx.io.files.Path(Path.of(cqlArgs.content.cqlPath).toFile()));
            engine.getEnvironment().getLibraryManager().getLibrarySourceLoader().registerProvider(provider);
        }

        // Load measure once
        Measure measure = getMeasure(parser, args.measurePath, args.measureName);

        // Create measure processor once
        R4MeasureProcessor processor = getR4MeasureProcessor(evaluationSettings, repository);

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

        VersionedIdentifier identifier = new VersionedIdentifier().withId(cqlArgs.content.name);
        Set<String> expressions = cqlArgs.content.expression != null ? Set.of(cqlArgs.content.expression) : null;

        // Create output directories if needed
        if (args.cql.outputPath != null) {
            Files.createDirectories(Path.of(args.cql.outputPath));
        }
        if (args.reportPath != null) {
            Files.createDirectories(Path.of(args.reportPath));
        }

        // Process each context value iteratively
        for (var c : cqlArgs.parameters.context) {
            var subjectId = c.contextName + "/" + c.contextValue;

            // Check if already processed (skip if both output files exist)
            boolean cqlExists =
                    args.cql.outputPath != null && Files.exists(Path.of(args.cql.outputPath, c.contextValue + ".txt"));
            boolean reportExists =
                    args.reportPath != null && Files.exists(Path.of(args.reportPath, c.contextValue + ".json"));

            if (cqlExists && reportExists) {
                log.info("⏭\uFE0F  Skipping {} (already processed)", c.contextValue);
                continue;
            }

            log.info("▶\uFE0F  Evaluating {}...", c.contextValue);

            // Evaluate CQL for this context
            var contextParameter = Pair.<String, Object>of(c.contextName, c.contextValue);
            var cqlResult = engine.evaluate(identifier, expressions, contextParameter);

            // Create single-entry map for measure processor
            // The API requires a map, so we create a map with just this one result
            Map<String, EvaluationResult> resultMap = new HashMap<>();
            resultMap.put(subjectId, cqlResult);

            // Generate measure report for this context
            MeasureReport measureReport = processor.evaluateMeasureResults(
                    measure, start, end, "subject", Collections.singletonList(subjectId), resultMap);

            // Write measure report immediately
            if (args.reportPath != null) {
                var json = parser.encodeResourceToString(measureReport);
                writeMeasureReportToFile(json, c.contextValue, Path.of(args.reportPath));
                log.info("Measure report for {} written to: {}", c.contextValue, args.reportPath);
            } else {
                // Write to stdout if no report path specified
                log.info(parser.encodeResourceToString(measureReport));
            }

            // Write CQL results immediately
            if (args.cql.outputPath != null) {
                var path = Path.of(args.cql.outputPath, c.contextValue + ".txt");
                try (OutputStream out = Files.newOutputStream(
                        path,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    Utilities.writeResult(cqlResult, out);
                    log.info("CQL results for {} written to: {}", c.contextValue, path);
                }
            }

            log.info("✅ Completed {}", c.contextValue);

            // Results go out of scope here, allowing GC to free memory
        }
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
            EvaluationSettings evaluationSettings, IRepository repository) {

        MeasureEvaluationOptions evaluationOptions = new MeasureEvaluationOptions();
        evaluationOptions.setApplyScoringSetMembership(false);
        evaluationOptions.setEvaluationSettings(evaluationSettings);

        return new R4MeasureProcessor(repository, evaluationOptions, new MeasureProcessorUtils());
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
