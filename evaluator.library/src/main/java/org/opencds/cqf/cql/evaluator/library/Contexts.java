package org.opencds.cqf.cql.evaluator.library;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.model.Model;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.builder.RetrieveProviderConfig;
import org.opencds.cqf.cql.evaluator.builder.data.FhirModelResolverFactory;
import org.opencds.cqf.cql.evaluator.builder.data.RetrieveProviderConfigurer;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.PriorityRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.retrieve.RepositoryRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.Constants;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;

public class Contexts {

  private Contexts() {}

  public static LibraryEvaluator forRepository(FhirContext fhirContext, Library library,
      Repository repository, IBaseBundle additionalData) {
    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    return forRepository(fhirContext, library, repository, additionalData, librarySourceProviders,
        null);
  }

  public static LibraryEvaluator forRepository(FhirContext fhirContext, Library library,
      Repository repository, IBaseBundle additionalData,
      List<LibrarySourceProvider> librarySourceProviders,
      CqlFhirParametersConverter cqlFhirParametersConverter) {
    checkNotNull(fhirContext);
    checkNotNull(repository);
    checkNotNull(librarySourceProviders);

    if (cqlFhirParametersConverter == null) {
      cqlFhirParametersConverter = getCqlFhirParametersConverter(fhirContext);
    }

    // Context context = new Context(library);
    var terminologyProvider = new RepositoryTerminologyProvider(fhirContext, repository);
    librarySourceProviders.add(buildLibrarySource(fhirContext, repository));
    var libraryLoader = buildLibraryLoader(librarySourceProviders);

    var dataProviders =
        buildDataProviders(fhirContext, repository, additionalData, terminologyProvider);
    var cqlEvaluator = new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider,
        cqlOptions.getCqlEngineOptions().getOptions());

    return new LibraryEvaluator(cqlFhirParametersConverter, cqlEvaluator);
  }

  private static Map<ModelIdentifier, Model> globalModelCache = new ConcurrentHashMap<>();

  private static Map<VersionedIdentifier, Library> libraryCache;

  private static CqlOptions cqlOptions = CqlOptions.defaultOptions();

  private static LibrarySourceProvider buildLibrarySource(FhirContext fhirContext,
      Repository repository) {
    AdapterFactory adapterFactory = getAdapterFactory(fhirContext);
    return new RepositoryFhirLibrarySourceProvider(fhirContext, repository, adapterFactory,
        new LibraryVersionSelector(adapterFactory));
  }

  // TODO: Add NPM library source loader support
  private static LibraryLoader buildLibraryLoader(
      List<LibrarySourceProvider> librarySourceProviders) {
    if (cqlOptions.useEmbeddedLibraries()) {
      librarySourceProviders.add(new FhirLibrarySourceProvider());
    }

    TranslatorOptionAwareLibraryLoader libraryLoader =
        new TranslatingLibraryLoader(new CacheAwareModelManager(globalModelCache),
            librarySourceProviders, cqlOptions.getCqlTranslatorOptions(), null);

    if (libraryCache != null) {
      libraryLoader = new CacheAwareLibraryLoaderDecorator(libraryLoader, libraryCache);
    }

    return libraryLoader;
  }

  private static Map<String, DataProvider> buildDataProviders(FhirContext fhirContext,
      Repository repository, IBaseBundle additionalData, TerminologyProvider terminologyProvider) {
    Map<String, DataProvider> dataProviders = new HashMap<>();

    var providers = new ArrayList<RetrieveProvider>();
    var modelResolver = new FhirModelResolverFactory()
        .create(fhirContext.getVersion().getVersion().getFhirVersionString());
    var retrieveProvider = new RepositoryRetrieveProvider(fhirContext, repository);
    providers.add(retrieveProvider);
    if (additionalData != null) {
      providers.add(new BundleRetrieveProvider(fhirContext, additionalData));
    }

    var retrieveProviderConfigurer =
        new RetrieveProviderConfigurer(RetrieveProviderConfig.defaultConfig());
    for (RetrieveProvider provider : providers) {
      retrieveProviderConfigurer.configure(provider, terminologyProvider);
    }

    dataProviders.put(Constants.FHIR_MODEL_URI,
        new CompositeDataProvider(modelResolver, new PriorityRetrieveProvider(providers)));

    return dataProviders;
  }

  public static AdapterFactory getAdapterFactory(FhirContext fhirContext) {
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
      case R4:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
      case R5:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory();
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
  }

  public static CqlFhirParametersConverter getCqlFhirParametersConverter(FhirContext fhirContext) {
    var fhirTypeConverter =
        new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    return new CqlFhirParametersConverter(fhirContext, getAdapterFactory(fhirContext),
        fhirTypeConverter);
  }
}
