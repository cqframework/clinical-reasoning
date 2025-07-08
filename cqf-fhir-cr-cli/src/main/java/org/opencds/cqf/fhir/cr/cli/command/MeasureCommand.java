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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Callable;
import org.hl7.fhir.r4.model.Measure;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.cli.argument.CqlArgument;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.SubjectProviderOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.R4RepositorySubjectProvider;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "measure",
        mixinStandardHelpOptions = true,
        description = "Evaluate FHIR Measures against FHIR resources.")
public class MeasureCommand implements Callable<Integer> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MeasureCommand.class);

    @ArgGroup(multiplicity = "1..1", exclusive = false)
    public CqlArgument cql;

    @ArgGroup(multiplicity = "1..1", exclusive = false)
    public MeasureArgument measure;

    static class MeasureArgument {
        @Option(
                names = {"-msr", "--measure"},
                description = "Provides the name of the measure to evaluate.")
        public String measureName;

        @Option(
                names = {"-ps", "--period-start"},
                description = "Specifies the start of the evaluation period.")
        public String periodStart;

        @Option(
                names = {"-pe", "--period-end"},
                description = "Specifies the end of the evaluation period.")
        public String periodEnd;

        @Option(
                names = {"--measure-path"},
                description = "Specifies the path to the measure resource.")
        public String measurePath;

        @Option(
                names = {"--single-file"},
                description = "Indicates whether to use a single output file for the evaluation results.")
        public boolean singleFile;

        @Option(
                names = {"--report-path"},
                description = "Specifies the path to the report output directory.")
        public String reportPath;
    }

    @Override
    public Integer call() throws IOException {
        var results = CqlCommand.evaluate(this.cql);

        var fhirContext = FhirContext.forCached(FhirVersionEnum.valueOf(cql.fhir.fhirVersion));
        var parser = fhirContext.newJsonParser();
        var resource = getMeasure(parser, measure.measurePath, measure.measureName);
        var processor = getR4MeasureProcessor(
                Utilities.createEvaluationSettings(cql.library.libraryUrl, cql.hedisCompatibilityMode),
                Utilities.createRepository(fhirContext, cql.terminologyUrl, cql.model.modelUrl));

        var start = measure.periodStart != null
                ? LocalDate.parse(measure.periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay(ZoneId.systemDefault())
                : null;

        var end = measure.periodEnd != null
                ? LocalDate.parse(measure.periodEnd, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atTime(LocalTime.MAX)
                        .atZone(ZoneId.systemDefault())
                : null;

        for (var e : results.entrySet()) {
            var subjectId = e.getKey();

            // Something is incorrect here we should not be getting one single
            // MeasureReport per measure, but rather one per subject when the type
            // is subject.
            var report = processor.evaluateMeasureResults(
                    resource, start, end, "subject", Collections.singletonList(subjectId), results);

            var json = parser.encodeResourceToString(report);
            writeJsonToFile(json, subjectId, Path.of(measure.reportPath).resolve("measurereports"));
        }

        return 0; // Return an appropriate exit code
    }

    @Nullable
    private Measure getMeasure(IParser parser, String measurePath, String measureName) {
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
    private R4MeasureProcessor getR4MeasureProcessor(EvaluationSettings evaluationSettings, IRepository repository) {

        MeasureEvaluationOptions evaluationOptions = new MeasureEvaluationOptions();
        evaluationOptions.setApplyScoringSetMembership(false);
        evaluationOptions.setEvaluationSettings(evaluationSettings);

        return new R4MeasureProcessor(
                repository,
                evaluationOptions,
                new R4RepositorySubjectProvider(new SubjectProviderOptions()),
                new R4MeasureServiceUtils(repository));
    }

    private void writeJsonToFile(String json, String patientId, Path path) {
        Path outputPath = path.resolve(patientId + ".json");

        try {
            // Ensure parent directories exist
            Files.createDirectories(outputPath.getParent());

            // Write JSON to file
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                out.write(json.getBytes());
                log.info(path + " written to: " + outputPath.toAbsolutePath());
            }

        } catch (IOException e) {
            log.error("Failed to write JSON for patient " + patientId, e);
        }
    }
}
