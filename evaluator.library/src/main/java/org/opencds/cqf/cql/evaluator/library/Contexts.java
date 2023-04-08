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
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
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

  public static LibraryEvaluator forRepository(EvaluationSettings theSettings,
      Repository theRepository,
      IBaseBundle theAdditionalData) {
    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    return forRepository(theSettings, theRepository, theAdditionalData, librarySourceProviders,
        null);
  }

  public static LibraryEvaluator forRepository(EvaluationSettings theSettings,
      Repository theRepository,
      IBaseBundle theAdditionalData, List<LibrarySourceProvider> theLibrarySourceProviders,
      CqlFhirParametersConverter theCqlFhirParametersConverter) {
    checkNotNull(theSettings);
    checkNotNull(theRepository);
    checkNotNull(theLibrarySourceProviders);

    if (theCqlFhirParametersConverter == null) {
      theCqlFhirParametersConverter = getCqlFhirParametersConverter(theRepository.fhirContext());
    }

    var terminologyProvider = new RepositoryTerminologyProvider(theRepository);
    theLibrarySourceProviders.add(buildLibrarySource(theRepository));
    var libraryLoader = buildLibraryLoader(theSettings, theLibrarySourceProviders);

    var dataProviders = buildDataProviders(theRepository, theAdditionalData, terminologyProvider);
    var cqlEvaluator = new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider,
        theSettings.getEngineOptions().getOptions());

    return new LibraryEvaluator(theCqlFhirParametersConverter, cqlEvaluator);
  }

  private static Map<ModelIdentifier, Model> ourGlobalModelCache = new ConcurrentHashMap<>();

  private static LibrarySourceProvider buildLibrarySource(Repository theRepository) {
    AdapterFactory adapterFactory = getAdapterFactory(theRepository.fhirContext());
    return new RepositoryFhirLibrarySourceProvider(theRepository, adapterFactory,
        new LibraryVersionSelector(adapterFactory));
  }

  // TODO: Add NPM library source loader support
  private static LibraryLoader buildLibraryLoader(EvaluationSettings theSettings,
      List<LibrarySourceProvider> theLibrarySourceProviders) {
    if (theSettings.getCqlOptions().useEmbeddedLibraries()) {
      theLibrarySourceProviders.add(new FhirLibrarySourceProvider());
    }

    TranslatorOptionAwareLibraryLoader libraryLoader =
        new TranslatingLibraryLoader(new CacheAwareModelManager(ourGlobalModelCache),
            theLibrarySourceProviders, theSettings.getCqlOptions().getCqlTranslatorOptions(), null);

    if (theSettings.getLibraryCache() != null) {
      libraryLoader =
          new CacheAwareLibraryLoaderDecorator(libraryLoader, theSettings.getLibraryCache());
    }

    return libraryLoader;
  }

  private static Map<String, DataProvider> buildDataProviders(Repository theRepository,
      IBaseBundle theAdditionalData, TerminologyProvider theTerminologyProvider) {
    Map<String, DataProvider> dataProviders = new HashMap<>();

    var providers = new ArrayList<RetrieveProvider>();
    var modelResolver = new FhirModelResolverFactory()
        .create(theRepository.fhirContext().getVersion().getVersion().getFhirVersionString());
    var retrieveProvider = new RepositoryRetrieveProvider(theRepository);
    providers.add(retrieveProvider);
    if (theAdditionalData != null) {
      providers.add(new BundleRetrieveProvider(theRepository.fhirContext(), theAdditionalData));
    }

    var retrieveProviderConfigurer =
        new RetrieveProviderConfigurer(RetrieveProviderConfig.defaultConfig());
    for (RetrieveProvider provider : providers) {
      retrieveProviderConfigurer.configure(provider, theTerminologyProvider);
    }

    dataProviders.put(Constants.FHIR_MODEL_URI,
        new CompositeDataProvider(modelResolver, new PriorityRetrieveProvider(providers)));

    return dataProviders;
  }

  public static AdapterFactory getAdapterFactory(FhirContext theFhirContext) {
    switch (theFhirContext.getVersion().getVersion()) {
      case DSTU3:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3.AdapterFactory();
      case R4:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory();
      case R5:
        return new org.opencds.cqf.cql.evaluator.fhir.adapter.r5.AdapterFactory();
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", theFhirContext));
    }
  }

  public static CqlFhirParametersConverter getCqlFhirParametersConverter(
      FhirContext theFhirContext) {
    var fhirTypeConverter =
        new FhirTypeConverterFactory().create(theFhirContext.getVersion().getVersion());
    return new CqlFhirParametersConverter(theFhirContext, getAdapterFactory(theFhirContext),
        fhirTypeConverter);
  }
}
