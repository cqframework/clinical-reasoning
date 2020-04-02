package org.opencds.cqf.cql.evaluator.builder;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.data.DataProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderContext;
import org.opencds.cqf.cql.execution.LibraryLoader;
import org.opencds.cqf.cql.terminology.TerminologyProvider;

public class CqlEvaluatorBuilder {

// ---
//     .forLibrary(String)
//     .forLibraryUrl(URL)
//     .forLibraryBundle(STU3, R4)
// --
//     .withModelManagerExtensions()
//     .withLibraryManagerExtensions()
// ---
//     .withLibraryManager(LibraryManager)
// ---
//     .withLibraryLoaderExtensions()
// ---

//     .withLibraryLoader()

// --
//     //.forTerminology(String)
//     .forTerminologyBundle(STU3, R4)
//     .forTerminologyUrl(URL)
// ---
//     .withTerminologyProvider()

// --
//     .withTerminologyProviderExtensions()

// --
//    .for 

    private BuilderContext builderContext = new BuilderContext();

    public CqlEvaluatorBuilder(String libraryContent) {
        Objects.requireNonNull(libraryContent, "libraryContent can not be null");
    }

    public CqlEvaluatorBuilder(LibraryLoader libraryLoader, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraryLoader, "libraryLoader can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");
        
    }

    public CqlEvaluatorBuilder(List<String> libraryContent, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraryContent, "libraryContent can not be null");
        if (libraryContent.isEmpty()) {
            throw new IllegalArgumentException("libraryContent can not be empty");
        }

        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");

    }

    public CqlEvaluatorBuilder(URL libraryUrl, VersionedIdentifier libraryIdentifier) {
        Objects.requireNonNull(libraryUrl, "libraryUrl can not be null");
        Objects.requireNonNull(libraryIdentifier, "libraryIdentifier can not be null");

    }

    // private EnumSet<Options> evaluatorBuilderOptions;
    // public CqlEvaluatorBuilder withEvaluatorBuilderOptions(EnumSet<Options> evaluatorBuilderOptions) {
    //     Objects.requireNonNull(evaluatorBuilderOptions);
    //     if (this.evaluatorBuilderOptions != null) {
    //         throw new IllegalArgumentException("evaluatorBuilderOptions already set.");
    //     }

    //     this.evaluatorBuilderOptions = evaluatorBuilderOptions;

    //     return this;
    // }

    // private EnumSet<CqlEngine.Options> engineOptions;
    // public CqlEvaluatorBuilder withEngineOptions(EnumSet<CqlEngine.Options> engineOptions) {
    //     Objects.requireNonNull(engineOptions);
    //     if (this.engineOptions != null) {
    //         throw new IllegalArgumentException("engineOptions already set.");
    //     }

    //     this.engineOptions = engineOptions;

    //     return this;
    // }

    // private EnumSet<CqlTranslator.Options> translatorOptions;
    // public CqlEvaluatorBuilder withTranslatorOptions(EnumSet<CqlTranslator.Options> translatorOptions) {
    //     Objects.requireNonNull(translatorOptions);
    //     if (this.translatorOptions != null) {
    //         throw new IllegalArgumentException("translatorOptions already set.");
    //     }

    //     this.translatorOptions = translatorOptions;

    //     return this;
    // }

    // private TerminologyProvider terminologyProvider;
    // private TerminologyProviderFactory terminologyProviderFactory;
    // public CqlEvaluatorBuilder withTerminologyProviderFactory(TerminologyProviderFactory terminologyProviderFactory) {
    //     Objects.requireNonNull(terminologyProviderFactory);
    //     if (this.terminologyProviderFactory != null) {
    //         throw new IllegalArgumentException("terminologyProviderFactory already set.");
    //     }

    //     if (this.terminologyProvider != null) {
    //         throw new IllegalArgumentException("terminologyProvider already set. If a terminologyProvider is already set the factory will not be used.");
    //     }

    //     this.terminologyProviderFactory = terminologyProviderFactory;

    //     return this;
    // }

    // public CqlEvaluatorBuilder withTerminologyProvider(TerminologyProvider terminologyProvider) {
    //     Objects.requireNonNull(terminologyProvider);
    //     if (this.terminologyProvider != null) {
    //         throw new IllegalArgumentException("terminologyProvider already set.");
    //     }

    //     if (this.terminologyProviderFactory != null) {
    //         throw new IllegalArgumentException("terminologyProviderFactory already set. If a terminologyProviderFactory is already set the terminology provider will not be used.");
    //     }

    //     this.terminologyProvider = terminologyProvider;

    //     return this;
    // }

    // private DataProviderFactory dataProviderFactory;
    // private Map<String, DataProvider> dataProviders;
    // public CqlEvaluatorBuilder withDataProviderFactory(DataProviderFactory dataProviderFactory) {
    //     Objects.requireNonNull(dataProviderFactory);
    //     if (this.dataProviderFactory != null) {
    //         throw new IllegalArgumentException("dataProviderFactory already set.");
    //     }

    //     if (this.dataProviders != null) {
    //         throw new IllegalArgumentException("dataProviders already set. If dataProviders is already set the factory will not be used.");
    //     }

    //     this.dataProviderFactory = dataProviderFactory;

    //     return this;
    // }

    // private LibraryLoaderFactory libraryLoaderFactory;
    // private ParameterResolver parameterResolver;




    // public CqlEvaluatorBuilder() {
    //     this(null, null, null, null, null, null, null);
    // }

    // // Use this as a quick way to enable file uris.
    // public CqlEvaluatorBuilder(EnumSet<Options> options) {
    //     this(null, null, null, null, options, null, null);
    // }

    // public CqlEvaluatorBuilder(LibraryLoaderFactory libraryLoaderFactory, DataProviderFactory dataProviderFactory,
    //         TerminologyProviderFactory terminologyProviderFactory, ParameterResolver parameterResolver,
    //         EnumSet<Options> options, EnumSet<CqlEngine.Options> engineOptions,
    //         EnumSet<CqlTranslator.Options> translatorOptions) {

    //     if (libraryLoaderFactory == null) {
    //         libraryLoaderFactory = new DefaultLibraryLoaderFactory();
    //     }

    //     if (dataProviderFactory == null) {
    //         dataProviderFactory = new DefaultDataProviderFactory();
    //     }

    //     if (terminologyProviderFactory == null) {
    //         terminologyProviderFactory = new DefaultTerminologyProviderFactory();
    //     }

    //     if (parameterResolver == null) {
    //         parameterResolver = new DefaultParameterResolver();
    //     }

    //     if (engineOptions == null) {
    //         engineOptions = EnumSet.of(org.opencds.cqf.cql.execution.CqlEngine.Options.EnableExpressionCaching);
    //     }

    //     if (options == null) {
    //         options = EnumSet.noneOf(Options.class);
    //     }

    //     if (translatorOptions == null) {
    //         // Default for measure eval
    //         translatorOptions = EnumSet.of(CqlTranslator.Options.EnableAnnotations,
    //                 CqlTranslator.Options.EnableLocators, CqlTranslator.Options.DisableListDemotion,
    //                 CqlTranslator.Options.DisableListPromotion, CqlTranslator.Options.DisableMethodInvocation);
    //     }

    //     this.libraryLoaderFactory = libraryLoaderFactory;
    //     this.dataProviderFactory = dataProviderFactory;
    //     this.terminologyProviderFactory = terminologyProviderFactory;
    //     this.parameterResolver = parameterResolver;
    //     this.options = options;
    //     this.engineOptions = engineOptions;
    //     this.translatorOptions = translatorOptions;
    // }

    // withClientFactory(ClientFactory)

    // withLibraryLoaderFactory(libraryLoaderFactory)
    // withDataProviderFactory()
    // withTerminologyProviderFactory()

    // withLibraries(List<String>)
    // withLibrarySource(url)
    // withLibraryLoader(LibraryLoader)

    // withTerminology(Bundle)
    // withTerminologySource(url)
    // withTerminologyProvider(terminologyProvider)

    // withData(Map<String, Object>)
    // withDataSources(List<ModelInfo>)
    // withDataProviders(Map<String, DataProvider>



    public CqlEvaluator build() {
        validateBuilderContext();

        LibraryLoader libraryLoader = this.builderContext.getLibraryContext().buildLibraryLoader();
        VersionedIdentifier primaryLibrary = null;

        TerminologyProvider terminologyProvider = this.builderContext.getTerminologyContext().buildTerminologyProvider();

        this.builderContext.getDataContext().setTerminologyProver(terminologyProvider);

        Map<String, DataProvider> dataProviders = this.builderContext.getDataContext().buildDataProviders();

        return new CqlEvaluator(libraryLoader, primaryLibrary, terminologyProvider, dataProviders, this.builderContext.getEngineOptions(), this.builderContext.getParameterResolver());
    }

    public void validateBuilderContext() {

    }

    // private void ensureNotFileUri(String uri) {
    //     if (Helpers.isFileUri(uri)) {
    //         throw new IllegalArgumentException(String.format("%s is not a valid uri", uri));
    //     }
    // }

    //     validateParameters(parameters);

    //     LibraryLoader libraryLoader = null;
    //     if (parameters.libraryPath != null && !parameters.libraryPath.isEmpty()) {
    //         libraryLoader = this.libraryLoaderFactory.create(parameters.libraryPath, this.translatorOptions);
    //     }
    //     else {
    //         libraryLoader = this.libraryLoaderFactory.create(parameters.libraries, this.translatorOptions);
    //     }

    //     Map<VersionedIdentifier, Set<String>> expressions = this.toExpressionMap(parameters.expressions);
    //     Map<VersionedIdentifier, Map<String, Object>> evaluationParameters = this.toParameterMap(parameters.parameters);

    //     // TOOD: Recursively resolve ALL libraries, not just those that are used by parameters, expressions, and library name.
    //     // Either that or have the library manager just give them all to us.

    //     Map<VersionedIdentifier, Library> libraries = new HashMap<VersionedIdentifier, Library>();
    //     if (parameters.libraryName != null) {
    //         Library lib = libraryLoader.load(toExecutionIdentifier(parameters.libraryName, parameters.libraryVersion));
    //         if (lib != null) {
    //             libraries.put(lib.getIdentifier(), li
    //         }
    //     }

    //     for (VersionedIdentifier v : expressions.keySet()) {
    //         Library lib = libraryLoader.load(v);
    //         if (lib != null && !libraries.containsKey(lib.getIdentifier())) {
    //             libraries.put(lib.getIdentifier(), lib);
    //         }
    //     }

    //     for (VersionedIdentifier v : evaluationParameters.keySet()) {
    //         Library lib = libraryLoader.load(v);
    //         if (lib != null && !libraries.containsKey(lib.getIdentifier())) {
    //             libraries.put(lib.getIdentifier(), lib);
    //         }
    //     }

    //     parameters.models = resolveModelVersions(libraries, parameters.models);

    //     // HACK AROUND: The file-based fhir terminology provider doesn't yet know how to auto-detect
    //     // Fhir version for json or xml resources, so we have to tell it explicitly what context to you.
    //     FhirContext context = null;
    //     Optional<ModelInfo> fhirInfo = parameters.models.stream().filter(x -> x.getName().equals("http://hl7.org/fhir")).findFirst();
    //     if (fhirInfo.isPresent()) {
    //         context = FhirVersionEnum.forVersionString(fhirInfo.get().getVersion()).newContext();
    //     }
    //     TerminologyProvider terminologyProvider = this.terminologyProviderFactory.create(context, parameters.terminologyUrl, parameters.clientFactory);
    //     Map<String, DataProvider> dataProviders = this.dataProviderFactory.create(parameters.models, terminologyProvider, parameters.clientFactory);

    //     Map<String, Object> resolvedContextParameters = this.parameterResolver.resolvecontextParameters(parameters.contextParameters);
    //     Map<VersionedIdentifier, Map<String, Object>> resolvedEvaluationParameters = this.parameterResolver.resolveParameters(libraries, evaluationParameters);

    //     CqlEngine engine = new CqlEngine(libraryLoader, dataProviders, terminologyProvider, this.engineOptions);

    //     EvaluationResult result = null;
    //     if (parameters.libraryName != null) {
    //         result = engine.evaluate(resolvedContextParameters, resolvedEvaluationParameters,
    //                 this.toExecutionIdentifier(parameters.libraryName, null));
    //     } else {
    //         result = engine.evaluate(resolvedContextParameters, resolvedEvaluationParameters, expressions);
    //     }

    //     Response response = new Response();
    //     response.evaluationResult = result;

    //     // TODO: Non-static serializers for different models.
    //     // Pair<String, String> versionAndUrl = modelVersionAndUrls.get("FHIR");
    //     // if (versionAndUrl != null) {
    //     //     EvaluationResultsSerializer.setFhirContext(versionAndUrl.getLeft());
    //     // }

    //     return response;
    // }

    // private List<ModelInfo> resolveModelVersions(Map<VersionedIdentifier, Library> libraries, List<ModelInfo> models) {

    //     models = this.expandAliasToUri(models);
    //     Map<String, Pair<String, String>> versions = new HashMap<>();
    //     for (Library library : libraries.values()) {
    //         if (library.getUsings() != null && library.getUsings().getDef() != null) {
    //             for (UsingDef u : library.getUsings().getDef()) {
    //                 String uri = u.getUri();
    //                 // Skip the system URI
    //                 if (uri.equals("urn:hl7-org:elm-types:r1")) {
    //                     continue;
    //                 }
    //                 String version = u.getVersion();
    //                 if (versions.containsKey(uri)) {
    //                     Pair<String, String> existing = versions.get(uri);
    //                     if (!existing.getLeft().equals(version)) {
    //                         throw new IllegalArgumentException(String.format(
    //                                 "Libraries are using multiple versions of %s. Only one version is supported at a time.",
    //                                 uri));
    //                     }
    //                 } else {
    //                     String url = models.stream().filter(x -> x.getName().equals(uri)).findFirst().get().getUrl();
    //                     versions.put(uri, Pair.of(version, url));
    //                 }
    //             }
    //         }
    //     }

    //     return versions.entrySet().stream()
    //             .map(x -> new ModelInfo(x.getKey(), x.getValue().getLeft(), x.getValue().getRight()))
    //             .collect(Collectors.toList());
    // }

    // private List<ModelInfo> expandAliasToUri(List<ModelInfo> models) {
    //     final Map<String, String> aliasMap = new HashMap<String, String>() {
    //         {
    //             put("FHIR", "http://hl7.org/fhir");
    //             put("QUICK", "http://hl7.org/fhir");
    //             put("QDM", "urn:healthit-gov:qdm:v5_4");
    //         }
    //     };

    //     if (models == null) {
    //         return null;
    //     }

    //     for (ModelInfo m : models) {
    //         if (aliasMap.containsKey(m.getName())) {
    //             m.setName(aliasMap.get(m.getName()));
    //         }
    //     }

    //     return models;
    // }

    // public VersionedIdentifier toExecutionIdentifier(String name, String version) {
    //     return new VersionedIdentifier().withId(name).withVersion(version);
    // }

    // private Map<VersionedIdentifier, Set<String>> toExpressionMap(List<ExpressionInfo> expressions) {
    //     Map<VersionedIdentifier, Set<String>> map = new HashMap<>();
    //     for (ExpressionInfo e : expressions) {
    //         VersionedIdentifier vi = toExecutionIdentifier(e.getLibraryName(), null);
    //         if (!map.containsKey(vi)) {
    //             map.put(vi, new HashSet<>());
    //         }

    //         map.get(vi).add(e.getIdentifier());
    //     }

    //     return map;
    // }



    // private void ensureNotFileUri(String uri) {
    //     if (Helpers.isFileUri(uri)) {
    //         throw new IllegalArgumentException(String.format("%s is not a valid uri", uri));
    //     }
    // }

    // private void ensureNotFileUri(Collection<String> uris) {
    //     for (String s : uris) {
    //         ensureNotFileUri(s);
    //     }
    // }

    // private void validateParameters(BuilderParameters parameters) {
    //     // Ensure EnableFileURI option is respected. This is a potential security risk
    //     // on a public server, so this must remain implemented.
    //     if (!this.options.contains(Options.EnableFileUri)) {
    //         ensureNotFileUri(parameters.libraryPath);
    //         ensureNotFileUri(parameters.terminologyUrl);
    //         if (parameters.models != null) {
    //             ensureNotFileUri(parameters.models.stream().map(x -> x.getUrl()).collect(Collectors.toList()));
    //         }
    //     }

    //     if (parameters.libraryName == null && (parameters.expressions == null || parameters.expressions.isEmpty())) {
    //         throw new IllegalArgumentException("libraryName or expressions must be specified.");
    //     }

    //     if (parameters.libraryName != null && (parameters.expressions != null && !parameters.expressions.isEmpty())) {
    //         throw new IllegalArgumentException("libraryName and expressions are mutually exclusive. Only specify one.");
    //     }

    //     if ((parameters.libraries != null && !parameters.libraries.isEmpty())
    //             && (parameters.libraryPath != null && !parameters.libraryPath.isEmpty())) {
    //         throw new IllegalArgumentException("libraries and library path are mutually exclusive. Only specify one.");
    //     }
    // }

    // public EnumSet<Options> getOptions() {
    //     return options;
    // }

    // public void setOptions(EnumSet<Options> options) {
    //     this.options = options;
    // }

    // public CqlEvaluatorBuilder withOptions(EnumSet<Options> options) {
    //     this.setOptions(options);
    //     return this;
    // }

    // public EnumSet<org.opencds.cqf.cql.execution.CqlEngine.Options> getEngineOptions() {
    //     return engineOptions;
    // }

    // public void setEngineOptions(EnumSet<org.opencds.cqf.cql.execution.CqlEngine.Options> engineOptions) {
    //     this.engineOptions = engineOptions;
    // }

    // public CqlEvaluatorBuilder withEngineOptions(EnumSet<org.opencds.cqf.cql.execution.CqlEngine.Options> engineOptions) {
    //     this.setEngineOptions(engineOptions);
    //     return this;
    // }

    // public EnumSet<CqlTranslator.Options> getTranslatorOptions() {
    //     return translatorOptions;
    // }

    // public void setTranslatorOptions(EnumSet<CqlTranslator.Options> translatorOptions) {
    //     this.translatorOptions = translatorOptions;
    // }

    // public CqlEvaluatorBuilder withTranslatorOptions(EnumSet<CqlTranslator.Options> translatorOptions) {
    //     this.setTranslatorOptions(translatorOptions);
    //     return this;
    // }

    // public TerminologyProviderFactory getTerminologyProviderFactory() {
    //     return terminologyProviderFactory;
    // }

    // public void setTerminologyProviderFactory(TerminologyProviderFactory terminologyProviderFactory) {
    //     this.terminologyProviderFactory = terminologyProviderFactory;
    // }

    // public CqlEvaluatorBuilder withTerminologyProviderFactory(TerminologyProviderFactory terminologyProviderFactory) {
    //     this.terminologyProviderFactory = terminologyProviderFactory;
    //     return this;
    // }

    // public DataProviderFactory getDataProviderFactory() {
    //     return dataProviderFactory;
    // }

    // public void setDataProviderFactory(DataProviderFactory dataProviderFactory) {
    //     this.dataProviderFactory = dataProviderFactory;
    // }

    // public CqlEvaluatorBuilder withDataProviderFactory(DataProviderFactory dataProviderFactory) {
    //     this.setDataProviderFactory(dataProviderFactory);
    //     return this;
    // }

    // public LibraryLoaderFactory getLibraryLoaderFactory() {
    //     return libraryLoaderFactory;
    // }

    // public void setLibraryLoaderFactory(LibraryLoaderFactory libraryLoaderFactory) {
    //     this.libraryLoaderFactory = libraryLoaderFactory;
    // }

    // public CqlEvaluatorBuilder withLibraryLoaderFactory(LibraryLoaderFactory libraryLoaderFactory) {
    //     this.libraryLoaderFactory = libraryLoaderFactory;
    //     return this;
    // }

    // public ParameterResolver getParameterResolver() {
    //     return parameterResolver;
    // }

    // public void setParameterResolver(ParameterResolver parameterResolver) {
    //     this.parameterResolver = parameterResolver;
    // }

    // public CqlEvaluatorBuilder withParameterResolver(ParameterResolver parameterResolver) {
    //     this.setParameterResolver(parameterResolver);
    //     return this;
    // }
}