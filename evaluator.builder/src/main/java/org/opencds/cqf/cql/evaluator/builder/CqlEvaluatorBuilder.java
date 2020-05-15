package org.opencds.cqf.cql.evaluator.builder;

import java.util.Map;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.builder.context.BuilderLibraryContext;

/**
 * API for Building any Providers or Loaders needed for CQL Evaluation
 */
public class CqlEvaluatorBuilder extends BuilderLibraryContext {
    public CqlEvaluator build(String primaryLibrary) {
        validateBuilderContext();
        LibraryLoader libraryLoader = this.getLibraryLoader();
        TerminologyProvider terminologyProvider = this.getTerminologyProvider();
        Map<String, DataProvider> dataProviders = this.getDataProvider();
        return new CqlEvaluator(libraryLoader, primaryLibrary, dataProviders, terminologyProvider, this.getEngineOptions(), this.getPameterResolver());
    }
    
    public CqlEvaluator build(VersionedIdentifier primaryLibrary) {
        validateBuilderContext();
        LibraryLoader libraryLoader = this.getLibraryLoader();
        TerminologyProvider terminologyProvider = this.getTerminologyProvider();
        Map<String, DataProvider> dataProviders = this.getDataProvider();
        return new CqlEvaluator(libraryLoader, primaryLibrary, dataProviders, terminologyProvider, this.getEngineOptions(), this.getPameterResolver());
    }

    /**
     * 
     */
    public void validateBuilderContext() {

    }

    public void test() {
        
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

    // private void ensureNotFileUri(String uri) {
    //     if (Helpers.isFileUri(uri)) {
    //         throw new IllegalArgumentException(String.format("%s is not a valid uri", uri));
    //     }
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