package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.base.Stopwatch;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlCompilerOptions.Options;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.CqlTranslatorOptionsMapper;
import org.cqframework.cql.cql2elm.DefaultLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r5.context.ILoggingService;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cql.CqlOptions;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.PROFILE_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.CODE_LOOKUP_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_MEMBERSHIP_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_PRE_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.cli.command.CqlCommand.EvaluationParameter.ModelParameter;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.opencds.cqf.fhir.cr.measure.SubjectProviderOptions;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.R4RepositorySubjectProvider;
import org.opencds.cqf.fhir.cr.measure.r4.utils.R4MeasureServiceUtils;
import org.opencds.cqf.fhir.utility.repository.ProxyRepository;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "cql", mixinStandardHelpOptions = true)
public class CqlCommand implements Callable<Integer> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CqlCommand.class);

    @Option(
            names = {"-fv", "--fhir-version"},
            required = true)
    public String fhirVersion;

    @Option(names = {"-op", "--options-path"})
    public String optionsPath;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public NamespaceParameter namespace;

    @Option(names = {"-rd", "--root-dir"})
    public String rootDir;

    @Option(names = {"-ig", "--ig-path"})
    public String igPath;

    @Option(names = {"-t", "--terminology-url"})
    public String terminologyUrl;

    @Option(names = {"-measure"})
    public String measureName;

    @Option(names = {"-periodStart"})
    public String periodStart;

    @Option(names = {"-periodEnd"})
    public String periodEnd;

    @Option(names = {"-measurePath"})
    public String measurePath;

    @Option(names = {"-singleFile"})
    public boolean singleFile = false;

    @Option(names = {"-resultsPath"})
    public String resultsPath;

    @ArgGroup(multiplicity = "1..1", exclusive = false)
    LibraryParameter library;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public ModelParameter model;

    @ArgGroup(multiplicity = "0..*", exclusive = false)
    public List<EvaluationParameter> evaluations;

    public static class NamespaceParameter {
        @Option(names = {"-nn", "--namespace-name"})
        public String namespaceName;

        @Option(names = {"-nu", "--namespace-uri"})
        public String namespaceUri;
    }

    public static class LibraryParameter {
        @Option(
                names = {"-lu", "--library-url"},
                required = true)
        public String libraryUrl;

        @Option(
                names = {"-ln", "--library-name"},
                required = true)
        public String libraryName;

        @Option(names = {"-lv", "--library-version"})
        public String libraryVersion;

        @Option(names = {"-e", "--expression"})
        public String[] expression;
    }

    public static class EvaluationParameter {
        @ArgGroup(multiplicity = "0..*", exclusive = false)
        public List<ParameterParameter> parameters;

        @ArgGroup(multiplicity = "0..1", exclusive = false)
        public ContextParameter context;

        public static class ContextParameter {
            @Option(names = {"-c", "--context"})
            public String contextName;

            @Option(names = {"-cv", "--context-value"})
            public String contextValue;
        }

        public static class ModelParameter {
            @Option(names = {"-m", "--model"})
            public String modelName;

            @Option(names = {"-mu", "--model-url"})
            public String modelUrl;
        }

        public static class ParameterParameter {
            @Option(names = {"-p", "--parameter"})
            public String parameterName;

            @Option(names = {"-pv", "--parameter-value"})
            public String parameterValue;
        }
    }

    @SuppressWarnings("removal")
    private static class Logger implements ILoggingService {

        private final org.slf4j.Logger log = LoggerFactory.getLogger(Logger.class);

        @Override
        public void logMessage(String s) {
            log.warn(s);
        }

        @Override
        public void logDebugMessage(LogCategory logCategory, String s) {
            log.debug("{}: {}", logCategory, s);
        }

        @Override
        public boolean isDebugLogging() {
            return log.isDebugEnabled();
        }
    }

    private String toVersionNumber(FhirVersionEnum fhirVersion) {
        return switch (fhirVersion) {
            case R4 -> "4.0.1";
            case R5 -> "5.0.0-ballot";
            case DSTU3 -> "3.0.2";
            default -> throw new IllegalArgumentException("Unsupported FHIR version %s".formatted(fhirVersion));
        };
    }

    @Override
    public Integer call() throws Exception {
        var watch = Stopwatch.createStarted();
        FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

        FhirContext fhirContext = FhirContext.forCached(fhirVersionEnum);

        var evaluationSettings = setupOptions(fhirVersionEnum);

        var repository = createRepository(fhirContext, terminologyUrl, model.modelUrl);
        VersionedIdentifier identifier = new VersionedIdentifier().withId(library.libraryName);

        var measureProcessor = getR4MeasureProcessor(evaluationSettings, repository);

        // hack to bring in Measure
        IParser parser = fhirContext.newJsonParser();

        var measure = getMeasure(parser);

        var initTime = watch.elapsed().toMillis();
        log.info("initialized in {} millis", initTime);
        AtomicInteger counter = new AtomicInteger(0);
        for (var e : evaluations) {
            String basePath = resultsPath;
            Path filepath = Path.of(basePath + this.library.libraryName, e.context.contextValue + ".txt");

            // ✅ Skip if already written
            if (Files.exists(filepath)) {
                log.info("⏭️ Skipping {} (already processed)", e.context.contextValue);
                continue;
            }
            var engine = Engines.forRepository(repository, evaluationSettings);
            // enable return all and equivalence
            engine.getState().getEngineOptions().add(CqlEngine.Options.EnableHedisCompatibilityMode);
            if (library.libraryUrl != null) {
                var provider = new DefaultLibrarySourceProvider(Path.of(library.libraryUrl));
                engine.getEnvironment()
                        .getLibraryManager()
                        .getLibrarySourceLoader()
                        .registerProvider(provider);
            }
            var subjectId = e.context.contextName + "/" + e.context.contextValue;
            log.info("evaluating: {}", subjectId);

            var evalStart = watch.elapsed().toMillis();
            var contextParameter = Pair.<String, Object>of(e.context.contextName, e.context.contextValue);
            var cqlResult = engine.evaluate(identifier, contextParameter);

            Map<String, EvaluationResult> result = new HashMap<>();
            result.put(subjectId, cqlResult);

            // generate MeasureReport from ExpressionResult
            if (measure != null) {
                String jsonReport;
                if (periodStart != null && periodEnd != null) {
                    var report = measureProcessor.evaluateMeasureResults(
                            measure,
                            LocalDate.parse(periodStart, DateTimeFormatter.ISO_LOCAL_DATE)
                                    .atStartOfDay(ZoneId.systemDefault()),
                            LocalDate.parse(periodEnd, DateTimeFormatter.ISO_LOCAL_DATE)
                                    .atTime(LocalTime.MAX)
                                    .atZone(ZoneId.systemDefault()),
                            "subject",
                            Collections.singletonList(subjectId),
                            result);

                    jsonReport = parser.encodeResourceToString(report);
                } else {
                    var report = measureProcessor.evaluateMeasureResults(
                            measure, null, null, "subject", Collections.singletonList(subjectId), result);
                    jsonReport = parser.encodeResourceToString(report);
                }

                writeJsonToFile(
                        jsonReport, e.context.contextValue, basePath + this.library.libraryName + "/measurereports");
            }

            if (singleFile) {
                // ✅ Write TXT result
                writeResultToFile(
                        cqlResult, e.context.contextValue, basePath + this.library.libraryName + "/txtresults");
            } else {
                writeResult(cqlResult);
            }
            var count = counter.incrementAndGet();

            var evalEnd = watch.elapsed().toMillis();
            log.info("evaluated #{} in {} millis", count, evalEnd - evalStart);
            log.info("avg (amortized across threads) {} millis", (evalEnd - initTime) / count);
        }

        var finalTime = watch.elapsed().toMillis();
        var elapsedTime = finalTime - initTime;
        log.info("evaluated in {} millis", elapsedTime);
        log.info("total time in {} millis", finalTime);
        log.info("per patient time in {} millis", elapsedTime / evaluations.size());

        return 0;
    }

    @Nullable
    private Measure getMeasure(IParser parser) throws FileNotFoundException {
        Measure measure = null;
        if (measureName != null && !measureName.contains("null")) {
            var measurePath = Path.of(this.measurePath, measureName + ".json");
            try (var is = Files.newInputStream(measurePath)) {
                measure = (Measure) parser.parseResource(is);
                if (measure == null) {
                    throw new IllegalArgumentException("measureName: %s not found".formatted(measureName));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("measurePath: %s not found".formatted(measurePath));
            }
        }
        return measure;
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

    @Nonnull
    private EvaluationSettings setupOptions(FhirVersionEnum fhirVersionEnum) {
        IGContext igContext = null;
        if (rootDir != null && igPath != null) {
            igContext = new IGContext(new Logger());
            igContext.initializeFromIg(rootDir, igPath, toVersionNumber(fhirVersionEnum));
        }

        CqlOptions cqlOptions = CqlOptions.defaultOptions();

        final CqlTranslatorOptions options;
        if (optionsPath != null) {
            options = CqlTranslatorOptionsMapper.fromFile(optionsPath);
        } else {
            options = CqlTranslatorOptions.defaultOptions();
        }

        options.getCqlCompilerOptions().getOptions().add(Options.EnableResultTypes);
        cqlOptions.setCqlCompilerOptions(options.getCqlCompilerOptions());

        var terminologySettings = new TerminologySettings();
        terminologySettings.setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);
        terminologySettings.setValuesetPreExpansionMode(VALUESET_PRE_EXPANSION_MODE.USE_IF_PRESENT);
        terminologySettings.setValuesetMembershipMode(VALUESET_MEMBERSHIP_MODE.USE_EXPANSION);
        terminologySettings.setCodeLookupMode(CODE_LOOKUP_MODE.USE_CODESYSTEM_URL);

        var retrieveSettings = new RetrieveSettings();
        retrieveSettings.setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        retrieveSettings.setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY);
        retrieveSettings.setProfileMode(PROFILE_MODE.DECLARED);

        var evaluationSettings = EvaluationSettings.getDefault();
        evaluationSettings.setCqlOptions(cqlOptions);
        evaluationSettings.setTerminologySettings(terminologySettings);
        evaluationSettings.setRetrieveSettings(retrieveSettings);
        evaluationSettings.setNpmProcessor(new NpmProcessor(igContext));

        return evaluationSettings;
    }

    private IRepository createRepository(FhirContext fhirContext, String terminologyUrl, String modelUrl) {
        IRepository data = null;
        IRepository terminology = null;

        if (modelUrl != null) {
            Path path = Path.of(modelUrl);
            data = new IgRepository(fhirContext, path);
        }

        if (terminologyUrl != null) {
            terminology = new IgRepository(fhirContext, Path.of(terminologyUrl));
        }

        return new ProxyRepository(data, null, terminology);
    }

    @SuppressWarnings("java:S106") // We are intending to output to the console here as a CLI tool
    private void writeResult(EvaluationResult result) {
        synchronized (System.out) {
            for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
                System.out.println(libraryEntry.getKey() + "="
                        + this.tempConvert(libraryEntry.getValue().value()));
            }

            System.out.println();
        }
    }

    private void writeJsonToFile(String json, String patientId, String path) {
        Path outputPath = Path.of(path, patientId + ".json");

        try {
            // Ensure parent directories exist
            Files.createDirectories(outputPath.getParent());

            // Write JSON to file
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                out.write(json.getBytes());
                log.info("✅ Saved MeasureReport to: {}", outputPath.toAbsolutePath());
            }

        } catch (IOException exception) {
            log.error("❌ Failed to write result for patient: {} to outputPath: {}", patientId, outputPath);

            throw new InvalidRequestException(
                    "Failed to write result for patient: %s to outputPath: %s".formatted(patientId, outputPath),
                    exception);
        }
    }

    private void writeResultToFile(EvaluationResult result, String patientId, String path) {
        Path outputPath = Path.of(path, patientId + ".txt");

        try {
            // Ensure parent directories exist
            Files.createDirectories(outputPath.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
                    String key = libraryEntry.getKey();
                    Object value = this.tempConvert(libraryEntry.getValue().value());
                    writer.write(key + "=" + value);
                    writer.newLine();
                }
            }

            log.info("✅ Wrote result to: {}", outputPath.toAbsolutePath());

        } catch (IOException exception) {
            log.error("❌ Failed to write result for patient: {} to outputPath: {}", patientId, outputPath);

            throw new InvalidRequestException(
                    "Failed to write result for patient: %s to outputPath: %s".formatted(patientId, outputPath),
                    exception);
        }
    }

    private String tempConvert(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof Iterable<?> values) {
            return StreamSupport.stream(values.spliterator(), false)
                    .map(this::tempConvert)
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        if (value instanceof IBaseResource resource) {
            return resource.fhirType()
                    + (resource.getIdElement() != null
                                    && resource.getIdElement().hasIdPart()
                            ? "(id=" + resource.getIdElement().getIdPart() + ")"
                            : "");
        }

        if (value instanceof IBaseDatatype datatype) {
            return datatype.fhirType();
        }

        if (value instanceof IBase base) {
            return base.fhirType();
        }

        return value.toString();
    }
}
