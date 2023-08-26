package org.opencds.cqf.fhir.cql;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;
import org.opencds.cqf.fhir.cql.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RepositoryRetrieveProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;

import ca.uhn.fhir.context.FhirContext;

public class Engines {

  private Engines() {}

  public static CqlEngine forRepository(Repository repository) {
    return forRepository(repository, EvaluationSettings.getDefault());
  }

  public static CqlEngine forRepository(Repository repository,
      EvaluationSettings settings) {
    return forRepository(repository, settings, true);
  }

  public static CqlEngine forRepository(Repository repository,
      EvaluationSettings settings, Boolean useLibraryCache) {
    var terminologyProvider = new RepositoryTerminologyProvider(repository);
    var sources = Collections.singletonList(buildLibrarySource(repository));

    var dataProviders = buildDataProviders(repository, null, terminologyProvider,
        settings.getRetrieveSettings());
    var environment =
        buildEnvironment(settings, sources, terminologyProvider, dataProviders, useLibraryCache);

    return new CqlEngine(environment, settings.getCqlOptions().getCqlEngineOptions().getOptions());
  }

  public static CqlEngine forRepositoryAndSettings(EvaluationSettings settings,
      Repository repository, IBaseBundle additionalData) {
    return forRepositoryAndSettings(settings, repository, additionalData, true);
  }

  public static CqlEngine forRepositoryAndSettings(EvaluationSettings settings,
      Repository repository, IBaseBundle additionalData, Boolean useLibraryCache) {
    checkNotNull(settings);
    checkNotNull(repository);

    var terminologyProvider = new RepositoryTerminologyProvider(repository);
    var sourceProviders = new ArrayList<LibrarySourceProvider>();
    sourceProviders.add(buildLibrarySource(repository));

    var dataProviders = buildDataProviders(repository, additionalData, terminologyProvider,
        settings.getRetrieveSettings());
    var environment =
        buildEnvironment(settings, sourceProviders, terminologyProvider, dataProviders,
            useLibraryCache);
    return new CqlEngine(environment,
        settings.getCqlOptions().getCqlEngineOptions().getOptions());
  }

  // public static LibraryEvaluator forRepository(EvaluationSettings settings, Repository
  // repository,
  // IBaseBundle additionalData, Boolean useLibraryCache) {
  // List<LibrarySourceProvider> librarySourceProviders = new ArrayList<>();
  // return forRepository(settings, repository, additionalData, librarySourceProviders,
  // null, useLibraryCache);
  // }

  // public static LibraryEvaluator forRepository(EvaluationSettings settings, Repository
  // repository,
  // IBaseBundle additionalData, List<LibrarySourceProvider> librarySourceProviders,
  // CqlFhirParametersConverter cqlFhirParametersConverter, Boolean useLibraryCache) {
  // checkNotNull(settings);
  // checkNotNull(repository);
  // checkNotNull(librarySourceProviders);

  // if (cqlFhirParametersConverter == null) {
  // cqlFhirParametersConverter = getCqlFhirParametersConverter(repository.fhirContext());
  // }

  // var terminologyProvider = new RepositoryTerminologyProvider(repository);
  // librarySourceProviders.add(buildLibrarySource(repository));

  // var dataProviders = buildDataProviders(repository, additionalData, terminologyProvider,
  // settings.getRetrieveSettings());
  // var environment =
  // buildEnvironment(settings, librarySourceProviders, terminologyProvider, dataProviders,
  // useLibraryCache);

  // var cqlEngine =
  // new CqlEngine(environment, settings.getCqlOptions().getCqlEngineOptions().getOptions());

  // return new LibraryEvaluator(cqlFhirParametersConverter, cqlEngine);
  // }

  private static LibrarySourceProvider buildLibrarySource(Repository repository) {
    AdapterFactory adapterFactory = getAdapterFactory(repository.fhirContext());
    return new RepositoryFhirLibrarySourceProvider(repository, adapterFactory,
        new LibraryVersionSelector(adapterFactory));
  }

  // TODO: Add NPM library source loader support
  private static Environment buildEnvironment(EvaluationSettings settings,
      List<LibrarySourceProvider> librarySourceProviders, TerminologyProvider terminologyProvider,
      Map<String, DataProvider> dataProviders, Boolean useLibraryCache) {
    if (settings.getCqlOptions().useEmbeddedLibraries()) {
      librarySourceProviders.add(new FhirLibrarySourceProvider());
    }

    var modelManager =
        settings.getModelCache() != null ? new ModelManager(settings.getModelCache())
            : new ModelManager();

    LibraryManager libraryManager =
        new LibraryManager(modelManager, settings.getCqlOptions().getCqlCompilerOptions(),
            Boolean.TRUE.equals(useLibraryCache) ? settings.getLibraryCache() : null);
    libraryManager.getLibrarySourceLoader().clearProviders();


    librarySourceProviders.forEach(lsp -> {
      libraryManager.getLibrarySourceLoader().registerProvider(lsp);
    });

    if (settings.getCqlOptions().useEmbeddedLibraries()) {
      libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
    }

    return new Environment(libraryManager, dataProviders, terminologyProvider);
  }

  private static Map<String, DataProvider> buildDataProviders(Repository repository,
      IBaseBundle additionalData, TerminologyProvider theTerminologyProvider,
      RetrieveSettings retrieveSettings) {
    Map<String, DataProvider> dataProviders = new HashMap<>();

    var providers = new ArrayList<RetrieveProvider>();
    var modelResolver = FhirModelResolverCache
        .resolverForVersion(repository.fhirContext().getVersion().getVersion());
    var retrieveProvider =
        new RepositoryRetrieveProvider(repository, retrieveSettings);
    providers.add(retrieveProvider);
    if (additionalData != null) {
      providers.add(new BundleRetrieveProvider(repository.fhirContext(), additionalData));
    }

    var retrieveProviderConfigurer =
        new RetrieveProviderConfigurerImpl(RetrieveProviderConfig.defaultConfig());
    for (RetrieveProvider provider : providers) {
      retrieveProviderConfigurer.configure(provider, theTerminologyProvider);
    }

    // TODO: Next repositories instead
    dataProviders.put(Constants.FHIR_MODEL_URI,
        new CompositeDataProvider(modelResolver, providers.get(0)));

    return dataProviders;
  }

  public static AdapterFactory getAdapterFactory(FhirContext fhirContext) {
    switch (fhirContext.getVersion().getVersion()) {
      case DSTU3:
        return new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
      case R4:
        return new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
      case R5:
        return new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
      default:
        throw new IllegalArgumentException(
            String.format("unsupported FHIR version: %s", fhirContext));
    }
  }

  public static CqlFhirParametersConverter getCqlFhirParametersConverter(
      FhirContext fhirContext) {
    var fhirTypeConverter =
        new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
    return new CqlFhirParametersConverter(fhirContext, getAdapterFactory(fhirContext),
        fhirTypeConverter);
  }
}
