package org.opencds.cqf.fhir.cql;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.fhir.npm.ILibraryReader;
import org.cqframework.fhir.npm.NpmLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmModelInfoProvider;
import org.cqframework.fhir.npm.NpmProcessor;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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
import org.opencds.cqf.fhir.cql.engine.retrieve.FederatedDataProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RepositoryRetrieveProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engines {

    private static Logger logger = LoggerFactory.getLogger(Engines.class);

    private Engines() {}

    public static CqlEngine forRepository(Repository repository) {
        return forRepository(repository, EvaluationSettings.getDefault());
    }

    public static CqlEngine forRepository(Repository repository, EvaluationSettings settings) {
        return forRepository(repository, settings, null, true);
    }

    public static CqlEngine forRepository(
            Repository repository, EvaluationSettings settings, NpmProcessor npmProcessor, Boolean useLibraryCache) {
        var terminologyProvider = new RepositoryTerminologyProvider(
                repository, settings.getValueSetCache(), settings.getTerminologySettings());
        var sources = new ArrayList<LibrarySourceProvider>();
        sources.add(buildLibrarySource(repository));

        var dataProviders = buildDataProviders(repository, null, terminologyProvider, settings.getRetrieveSettings());
        var environment =
                buildEnvironment(settings, sources, terminologyProvider, dataProviders, npmProcessor, useLibraryCache);

        return new CqlEngine(
                environment, settings.getCqlOptions().getCqlEngineOptions().getOptions());
    }

    public static CqlEngine forRepositoryAndSettings(
            EvaluationSettings settings, Repository repository, IBaseBundle additionalData) {
        return forRepositoryAndSettings(settings, repository, additionalData, null, true);
    }

    public static CqlEngine forRepositoryAndSettings(
            EvaluationSettings settings,
            Repository repository,
            IBaseBundle additionalData,
            NpmProcessor npmProcessor,
            Boolean useLibraryCache) {
        checkNotNull(settings);
        checkNotNull(repository);

        var terminologyProvider = new RepositoryTerminologyProvider(
                repository, settings.getValueSetCache(), settings.getTerminologySettings());
        var sourceProviders = new ArrayList<LibrarySourceProvider>();
        sourceProviders.add(buildLibrarySource(repository));

        var dataProviders =
                buildDataProviders(repository, additionalData, terminologyProvider, settings.getRetrieveSettings());
        var environment = buildEnvironment(
                settings, sourceProviders, terminologyProvider, dataProviders, npmProcessor, useLibraryCache);
        return new CqlEngine(
                environment, settings.getCqlOptions().getCqlEngineOptions().getOptions());
    }

    private static LibrarySourceProvider buildLibrarySource(Repository repository) {
        AdapterFactory adapterFactory = getAdapterFactory(repository.fhirContext());
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    // TODO: Add NPM library source loader support
    private static Environment buildEnvironment(
            EvaluationSettings settings,
            List<LibrarySourceProvider> librarySourceProviders,
            TerminologyProvider terminologyProvider,
            Map<String, DataProvider> dataProviders,
            NpmProcessor npmProcessor,
            Boolean useLibraryCache) {
        if (settings.getCqlOptions().useEmbeddedLibraries()) {
            librarySourceProviders.add(new FhirLibrarySourceProvider());
        }

        var modelManager =
                settings.getModelCache() != null ? new ModelManager(settings.getModelCache()) : new ModelManager();

        if (npmProcessor != null && npmProcessor.getIgContext() != null && npmProcessor.getPackageManager() != null) {
            ILibraryReader reader = new org.cqframework.fhir.npm.LibraryLoader(
                    npmProcessor.getIgContext().getFhirVersion());
            LoggerAdapter adapter = new LoggerAdapter(logger);
            librarySourceProviders.add(new NpmLibrarySourceProvider(
                    npmProcessor.getPackageManager().getNpmList(), reader, adapter));
            modelManager
                    .getModelInfoLoader()
                    .registerModelInfoProvider(new NpmModelInfoProvider(
                            npmProcessor.getPackageManager().getNpmList(), reader, adapter));
        }

        LibraryManager libraryManager = new LibraryManager(
                modelManager,
                settings.getCqlOptions().getCqlCompilerOptions(),
                Boolean.TRUE.equals(useLibraryCache) ? settings.getLibraryCache() : null);
        libraryManager.getLibrarySourceLoader().clearProviders();

        if (npmProcessor != null) {
            for (var n : npmProcessor.getNamespaces()) {
                libraryManager.getNamespaceManager().addNamespace(n);
            }
        }

        librarySourceProviders.forEach(lsp -> {
            libraryManager.getLibrarySourceLoader().registerProvider(lsp);
        });

        if (settings.getCqlOptions().useEmbeddedLibraries()) {
            libraryManager.getLibrarySourceLoader().registerProvider(new FhirLibrarySourceProvider());
        }

        return new Environment(libraryManager, dataProviders, terminologyProvider);
    }

    private static Map<String, DataProvider> buildDataProviders(
            Repository repository,
            IBaseBundle additionalData,
            TerminologyProvider terminologyProvider,
            RetrieveSettings retrieveSettings) {
        Map<String, DataProvider> dataProviders = new HashMap<>();

        var providers = new ArrayList<RetrieveProvider>();
        var modelResolver = FhirModelResolverCache.resolverForVersion(
                repository.fhirContext().getVersion().getVersion());
        // TODO: Make a federated repository here once that is ready for sure
        // var fedRepo = new FederatedRepository(repository, bundleRepo);
        var retrieveProvider = new RepositoryRetrieveProvider(repository, terminologyProvider, retrieveSettings);
        providers.add(retrieveProvider);
        if (additionalData != null && modelResolver.resolvePath(additionalData, "entry") != null) {
            var bundleRepo = new InMemoryFhirRepository(repository.fhirContext(), additionalData);
            var provider = new RepositoryRetrieveProvider(bundleRepo, terminologyProvider, retrieveSettings);
            providers.add(provider);
        }

        dataProviders.put(Constants.FHIR_MODEL_URI, new FederatedDataProvider(modelResolver, providers));

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
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
        }
    }

    public static CqlFhirParametersConverter getCqlFhirParametersConverter(FhirContext fhirContext) {
        var fhirTypeConverter =
                new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        return new CqlFhirParametersConverter(fhirContext, getAdapterFactory(fhirContext), fhirTypeConverter);
    }
}
