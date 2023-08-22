package org.opencds.cqf.cql.evaluator.cli.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.CqlTranslatorOptionsMapper;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.IGContext;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.CqlEvaluatorBuilder;
import org.opencds.cqf.cql.evaluator.builder.DataProviderComponents;
import org.opencds.cqf.cql.evaluator.builder.DataProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.builder.library.CqlFileLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.FhirFileLibrarySourceProviderFactory;
import org.opencds.cqf.cql.evaluator.builder.library.LibrarySourceProviderFactory;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.DirectoryBundler;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "cql", mixinStandardHelpOptions = true)
public class CqlCommand implements Callable<Integer> {
  @Option(names = {"-fv", "--fhir-version"}, required = true)
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
    @Option(names = {"-lu", "--library-url"}, required = true)
    public String libraryUrl;

    @Option(names = {"-ln", "--library-name"}, required = true)
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

  private Map<String, LibrarySourceProvider> librarySourceProviderIndex = new HashMap<>();

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
        throw new IllegalArgumentException(
            String.format("Unsupported FHIR version %s", fhirVersion));
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

    for (LibraryParameter library : libraries) {
      CqlEvaluatorBuilder cqlEvaluatorBuilder = new CqlEvaluatorBuilder();
      cqlEvaluatorBuilder.withCqlOptions(cqlOptions);

      if (namespace != null) {
        cqlEvaluatorBuilder
            .withNamespaceInfo(new NamespaceInfo(namespace.namespaceName, namespace.namespaceUri));
      }

      if (igContext != null) {
        cqlEvaluatorBuilder.withNpmProcessor(new NpmProcessor(igContext));
      }

      LibrarySourceProvider librarySourceProvider =
          librarySourceProviderIndex.get(library.libraryUrl);

      if (librarySourceProvider == null) {
        librarySourceProvider = createLibrarySourceProviderFactory(fhirContext)
            .create(new EndpointInfo().setAddress(library.libraryUrl));
        this.librarySourceProviderIndex.put(library.libraryUrl, librarySourceProvider);
      }

      cqlEvaluatorBuilder.withLibrarySourceProvider(librarySourceProvider);

      // TODO: Replace with FileRepository and proxied terminology provider
      // if (library.terminologyUrl != null) {
      // TerminologyProvider terminologyProvider =
      // this.terminologyProviderIndex.get(library.terminologyUrl);
      // if (terminologyProvider == null) {
      // terminologyProvider = createTerminologyProviderFactory(fhirContext)
      // .create(new EndpointInfo().setAddress(library.terminologyUrl));
      // this.terminologyProviderIndex.put(library.terminologyUrl, terminologyProvider);
      // }

      // cqlEvaluatorBuilder.withTerminologyProvider(terminologyProvider);
      // }

      DataProviderComponents dataProvider = null;
      DataProviderFactory dataProviderFactory = createDataProviderFactory(fhirContext);
      if (library.model != null) {
        dataProvider =
            dataProviderFactory.create(new EndpointInfo().setAddress(library.model.modelUrl));
      }
      // default to FHIR
      else {
        dataProvider =
            dataProviderFactory.create(new EndpointInfo().setType(Constants.HL7_FHIR_FILES_CODE));
      }

      cqlEvaluatorBuilder.withModelResolverAndRetrieveProvider(dataProvider.getModelUri(),
          dataProvider.getModelResolver(), dataProvider.getRetrieveProvider());

      CqlEngine evaluator = cqlEvaluatorBuilder.build();

      VersionedIdentifier identifier = new VersionedIdentifier().withId(library.libraryName);

      Pair<String, Object> contextParameter = null;

      if (library.context != null) {
        contextParameter = Pair.of(library.context.contextName, library.context.contextValue);
      }

      EvaluationResult result = evaluator.evaluate(identifier, contextParameter);

      writeResult(result);

    }

    return 0;
  }

  private LibrarySourceProviderFactory createLibrarySourceProviderFactory(
      FhirContext fhirContext) {
    var af = adapterFactory(fhirContext);
    var lvs = new LibraryVersionSelector(af);
    var db = directoryBundler(fhirContext);

    return new LibrarySourceProviderFactory(
        fhirContext, adapterFactory(fhirContext),
        Set.of(new FhirFileLibrarySourceProviderFactory(fhirContext, db, af, lvs),
            new CqlFileLibrarySourceProviderFactory()),
        lvs);
  }

  private org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory createDataProviderFactory(
      FhirContext fhirContext) {

    return null;
    // var db = directoryBundler(fhirContext);
    // return new org.opencds.cqf.cql.evaluator.builder.data.DataProviderFactory(fhirContext,
    // Set.of(new FhirModelResolverFactory()),
    // Set.of(new FhirFileRetrieveProviderFactory(fhirContext, db)));
  }

  private DirectoryBundler directoryBundler(FhirContext fhirContext) {
    return new DirectoryBundler(fhirContext);
  }

  private AdapterFactory adapterFactory(FhirContext fhirContext) {
    switch (fhirContext.getVersion().getVersion()) {

      case DSTU3:
        return new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
      case R4:
        return new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
      case R5:
        return new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
      case DSTU2:
      case DSTU2_1:
      case DSTU2_HL7ORG:
      default:
        throw new UnsupportedOperationException(String.format("FHIR version %s is not supported.",
            fhirContext.getVersion().getVersion().toString()));
    }
  }

  @SuppressWarnings("java:S106") // We are intending to output to the console here as a CLI tool
  private void writeResult(EvaluationResult result) {
    for (Map.Entry<String, ExpressionResult> libraryEntry : result.expressionResults.entrySet()) {
      System.out
          .println(libraryEntry.getKey() + "=" + this.tempConvert(libraryEntry.getValue().value()));
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
          + (resource.getIdElement() != null && resource.getIdElement().hasIdPart()
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
