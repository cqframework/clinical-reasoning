package org.opencds.cqf.cql.evaluator.library;

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
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.CqlOptions;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.model.CacheAwareModelManager;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.execution.CacheAwareLibraryLoaderDecorator;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatorOptionAwareLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory;
import org.opencds.cqf.fhir.api.Repository;

import ca.uhn.fhir.context.FhirContext;

public class Contexts {

  private Contexts() {}

  public static LibraryEvaluator forRepository(FhirContext fhirContext, Library library,
      Repository repository, IBaseBundle additionalData) {

    var fhirTypeConverter =
        new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    var cqlFhirParametersConverter = new CqlFhirParametersConverter(fhirContext,
        getAdapterFactory(fhirContext), fhirTypeConverter);
    // Context context = new Context(library);
    TerminologyProvider terminologyProvider =
        new RepositoryTerminologyProvider(fhirContext, repository);
    LibrarySourceProvider librarySourceProvider = buildLibrarySource(fhirContext, repository);
    LibraryLoader libraryLoader = buildLibraryLoader(librarySourceProvider);
    // RetrieveProvider retrieveProvider = new RepositoryRetrieveProvider(fhirContext, repository);
    // ModelResolver modelResolver = new FhirModelResolver<>(fhirContext);

    // context.registerLibraryLoader(libraryLoader);
    // context.registerTerminologyProvider(terminologyProvider);
    // context.registerDataProvider(Constants.FHIR_MODEL_URI,
    // new CompositeDataProvider(modelResolver, retrieveProvider));
    var dataProviders = buildDataProviders(repository, additionalData, terminologyProvider);
    var cqlEvaluator = new CqlEvaluator(libraryLoader, dataProviders, terminologyProvider,
        CqlOptions.defaultOptions().getCqlEngineOptions().getOptions());

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
  private static LibraryLoader buildLibraryLoader(LibrarySourceProvider librarySourceProvider) {
    List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
    librarySourceProviders.add(librarySourceProvider);
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

  private static Map<String, DataProvider> buildDataProviders(Repository repository,
      IBaseBundle additionalData, TerminologyProvider terminologyProvider) {
    Map<String, DataProvider> dataProviders = new HashMap<>();

    // dataProviders.put(null, null)
    // if (additionalData != null) {

    // }

    return dataProviders;
  }

  private static AdapterFactory getAdapterFactory(FhirContext fhirContext) {
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


}
