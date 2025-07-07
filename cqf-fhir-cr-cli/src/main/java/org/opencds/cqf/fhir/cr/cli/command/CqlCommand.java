package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.base.Stopwatch;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.BufferedWriter;
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
    public String compilerOptionsPath;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public NamespaceArgument namespaceArgument;

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
    LibraryArgument libraryArgument;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public ModelArgument modelArgument;

    @ArgGroup(multiplicity = "0..*", exclusive = false)
    public List<EvaluationArgument> evaluationArguments;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public CqlEngineArguments cqlEngineArguments;

    static class NamespaceArgument {
        @Option(names = {"-nn", "--namespace-name"})
        public String namespaceName;

        @Option(names = {"-nu", "--namespace-uri"})
        public String namespaceUri;
    }

    static class LibraryArgument {
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

    static class ModelArgument {
        @Option(names = {"-m", "--model"})
        public String modelName;

        @Option(names = {"-mu", "--model-url"})
        public String modelUrl;
    }

    static class EvaluationArgument {
        @ArgGroup(multiplicity = "0..*", exclusive = false)
        public List<ParameterArgument> parameters;

        @ArgGroup(multiplicity = "0..1", exclusive = false)
        public ContextArgument context;

        public static class ContextArgument {
            @Option(names = {"-c", "--context"})
            public String contextName;

            @Option(names = {"-cv", "--context-value"})
            public String contextValue;
        }

        static class ParameterArgument {
            @Option(names = {"-p", "--parameter"})
            public String parameterName;

            @Option(names = {"-pv", "--parameter-value"})
            public String parameterValue;
        }
    }

    static class CqlEngineArguments {
        @Option(names = {"--enable-hedis-compatibility-mode"})
        public boolean hedisCompatibilityMode;
    }

    private static class Logger implements ILoggingService {
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
        switch (fhirVersion) {
            case R4:
                return "4.0.1";
            case R5:
                return "5.0.0";
            case DSTU3:
                return "3.0.2";
            default:
                throw new IllegalArgumentException("Unsupported FHIR version %s".formatted(fhirVersion));
        }
    }

    @Override
    public Integer call() throws Exception {

        log.error("initializing");
        var watch = Stopwatch.createStarted();
        FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);
        FhirContext fhirContext = FhirContext.forCached(fhirVersionEnum);

        var evaluationSettings = setupOptions(fhirVersionEnum);

        var repository = createRepository(fhirContext, terminologyUrl, modelArgument.modelUrl);
        VersionedIdentifier identifier = new VersionedIdentifier().withId(libraryArgument.libraryName);

        var measureProcessor = getR4MeasureProcessor(evaluationSettings, repository);

        // hack to bring in Measure
        IParser parser = fhirContext.newJsonParser();

        var measure = getMeasure(parser);

        var initTime = watch.elapsed().toMillis();
        log.info("initialized in {} millis", initTime);
        AtomicInteger counter = new AtomicInteger(0);
        for (var e : evaluationArguments) {
            String basePath = resultsPath;
            Path filepath = Path.of(basePath + this.libraryArgument.libraryName, e.context.contextValue + ".txt");

            if (Files.exists(filepath)) {
                log.info("Skipping {} (already processed)", e.context.contextValue);
                continue;
            }
            var engine = Engines.forRepository(repository, evaluationSettings);

            if (libraryArgument.libraryUrl != null) {
                var provider = new DefaultLibrarySourceProvider(Path.of(libraryArgument.libraryUrl));
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
                        jsonReport,
                        e.context.contextValue,
                        getResultsPath(basePath).resolve("measurereports"));
            }

            if (singleFile) {
                writeResultToFile(
                        cqlResult,
                        e.context.contextValue,
                        getResultsPath(basePath).resolve("txtresults"));
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
        log.info("per patient time in {} millis", elapsedTime / evaluationArguments.size());

        return 0;
    }

    @Nonnull
    private Path getResultsPath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            basePath = System.getProperty("user.dir");
        }

        return Path.of(basePath, this.libraryArgument.libraryName);
    }

    @Nullable
    private Measure getMeasure(IParser parser) {
        Measure measure = null;
        if (measureName != null && !measureName.contains("null")) {
            var measureJsonFilePath = Path.of(this.measurePath, measureName + ".json");
            try (var is = Files.newInputStream(measureJsonFilePath)) {
                measure = (Measure) parser.parseResource(is);
                if (measure == null) {
                    throw new IllegalArgumentException("measureName: %s not found".formatted(measureName));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("measurePath: %s not found".formatted(measureJsonFilePath));
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
        if (cqlEngineArguments != null && cqlEngineArguments.hedisCompatibilityMode) {
            cqlOptions.getCqlEngineOptions().getOptions().add(CqlEngine.Options.EnableHedisCompatibilityMode);
        }

        if (compilerOptionsPath != null) {
            CqlTranslatorOptions options = CqlTranslatorOptionsMapper.fromFile(compilerOptionsPath);
            cqlOptions.setCqlCompilerOptions(options.getCqlCompilerOptions());
        }

        // Always add results types, since correct behavior of the CQL engine
        // depends on it.
        cqlOptions.getCqlCompilerOptions().getOptions().add(Options.EnableResultTypes);

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

    private void writeResultToFile(EvaluationResult result, String patientId, Path path) {
        Path outputPath = path.resolve(patientId + ".txt");

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

            log.info("Wrote result to: " + outputPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write result for patient " + patientId);
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
