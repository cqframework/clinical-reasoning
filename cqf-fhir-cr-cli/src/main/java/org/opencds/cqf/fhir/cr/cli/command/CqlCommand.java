package org.opencds.cqf.fhir.cr.cli.command;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.CqlTranslatorOptionsMapper;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cql.CqlOptions;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "cql", mixinStandardHelpOptions = true)
public class CqlCommand implements Callable<Integer> {
    @Option(
            names = {"-fv", "--fhir-version"},
            required = true)
    public String fhirVersion;

    @Option(names = {"-op", "--options-path"})
    public String optionsPath;

    @ArgGroup(multiplicity = "0..1", exclusive = false)
    public NamespaceParameter namespace;

    static class NamespaceParameter {
        @Option(names = {"-nn", "--namespace-name"})
        public String namespaceName;

        @Option(names = {"-nu", "--namespace-uri"})
        public String namespaceUri;
    }

    @Option(names = {"-rd", "--root-dir"})
    public String rootDir;

    @Option(names = {"-ig", "--ig-path"})
    public String igPath;

    @ArgGroup(multiplicity = "1..*", exclusive = false)
    List<LibraryParameter> libraries;

    static class LibraryParameter {
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

        @Option(names = {"-t", "--terminology-url"})
        public String terminologyUrl;

        @ArgGroup(multiplicity = "0..1", exclusive = false)
        public ModelParameter model;

        @ArgGroup(multiplicity = "0..*", exclusive = false)
        public List<ParameterParameter> parameters;

        @Option(names = {"-e", "--expression"})
        public String[] expression;

        @ArgGroup(multiplicity = "0..1", exclusive = false)
        public ContextParameter context;

        static class ContextParameter {
            @Option(names = {"-c", "--context"})
            public String contextName;

            @Option(names = {"-cv", "--context-value"})
            public String contextValue;
        }

        static class ModelParameter {
            @Option(names = {"-m", "--model"})
            public String modelName;

            @Option(names = {"-mu", "--model-url"})
            public String modelUrl;
        }

        static class ParameterParameter {
            @Option(names = {"-p", "--parameter"})
            public String parameterName;

            @Option(names = {"-pv", "--parameter-value"})
            public String parameterValue;
        }
    }

    private class Logger implements IWorkerContext.ILoggingService {

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
        switch (fhirVersion) {
            case R4:
                return "4.0.1";
            case R5:
                return "5.0.0-ballot";
            case DSTU3:
                return "3.0.2";
            default:
                throw new IllegalArgumentException(String.format("Unsupported FHIR version %s", fhirVersion));
        }
    }

    @Override
    public Integer call() throws Exception {

        FhirVersionEnum fhirVersionEnum = FhirVersionEnum.valueOf(fhirVersion);

        FhirContext fhirContext = FhirContext.forCached(fhirVersionEnum);

        IGContext igContext = null;
        if (rootDir != null && igPath != null) {
            igContext = new IGContext(new Logger());
            igContext.initializeFromIg(rootDir, igPath, toVersionNumber(fhirVersionEnum));
        }

        CqlOptions cqlOptions = CqlOptions.defaultOptions();

        if (optionsPath != null) {
            CqlTranslatorOptions options = CqlTranslatorOptionsMapper.fromFile(optionsPath);
            cqlOptions.setCqlCompilerOptions(options.getCqlCompilerOptions());
        }

        var evaluationSettings = EvaluationSettings.getDefault();
        evaluationSettings.setCqlOptions(cqlOptions);
        var repository = new IGFileStructureRepository(fhirContext, rootDir, IGLayoutMode.DIRECTORY, EncodingEnum.JSON);
        var engine = Engines.forRepositoryAndSettings(
                evaluationSettings, repository, null, new NpmProcessor(igContext), true);

        for (LibraryParameter library : libraries) {

            VersionedIdentifier identifier = new VersionedIdentifier().withId(library.libraryName);

            Pair<String, Object> contextParameter = null;

            if (library.context != null) {
                contextParameter = Pair.of(library.context.contextName, library.context.contextValue);
            }

            EvaluationResult result = engine.evaluate(identifier, contextParameter);

            writeResult(result);
        }

        return 0;
    }

    @SuppressWarnings("java:S106") // We are intending to output to the console here as a CLI tool
    private void writeResult(EvaluationResult result) {
        for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
            System.out.println(libraryEntry.getKey() + "="
                    + this.tempConvert(libraryEntry.getValue().value()));
        }

        System.out.println();
    }

    private String tempConvert(Object value) {
        if (value == null) {
            return "null";
        }

        String result = "";
        if (value instanceof Iterable) {
            result += "[";
            Iterable<?> values = (Iterable<?>) value;
            for (Object o : values) {

                result += (tempConvert(o) + ", ");
            }

            if (result.length() > 1) {
                result = result.substring(0, result.length() - 2);
            }

            result += "]";
        } else if (value instanceof IBaseResource) {
            IBaseResource resource = (IBaseResource) value;
            result = resource.fhirType()
                    + (resource.getIdElement() != null
                                    && resource.getIdElement().hasIdPart()
                            ? "(id=" + resource.getIdElement().getIdPart() + ")"
                            : "");
        } else if (value instanceof IBase) {
            result = ((IBase) value).fhirType();
        } else if (value instanceof IBaseDatatype) {
            result = ((IBaseDatatype) value).fhirType();
        } else {
            result = value.toString();
        }

        return result;
    }
}
