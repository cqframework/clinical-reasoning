package org.opencds.cqf.fhir.cql;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.fhir.npm.ILibraryReader;
import org.cqframework.fhir.npm.NpmLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmModelInfoProvider;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.hl7.cql.model.ModelInfoProvider;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirModelInfoProvider;
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter;
import org.opencds.cqf.fhir.cql.engine.retrieve.FederatedDataProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RepositoryRetrieveProvider;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings;
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class Engines {

    private static Logger logger = LoggerFactory.getLogger(Engines.class);

    private Engines() {}

    public static CqlEngine forRepository(IRepository repository) {
        return forRepository(repository, EvaluationSettings.getDefault());
    }

    public static CqlEngine forRepository(IRepository repository, EvaluationSettings settings) {
        return forRepository(repository, settings, null);
    }

    public static CqlEngine forRepository(
            IRepository repository, EvaluationSettings settings, IBaseBundle additionalData) {
        checkNotNull(settings);
        checkNotNull(repository);

        var terminologyProvider = new RepositoryTerminologyProvider(
                repository, settings.getValueSetCache(), settings.getTerminologySettings());
        var dataProviders =
                buildDataProviders(repository, additionalData, terminologyProvider, settings.getRetrieveSettings());
        var environment = buildEnvironment(repository, settings, terminologyProvider, dataProviders);
        return createEngine(environment, settings);
    }

    private static Environment buildEnvironment(
            IRepository repository,
            EvaluationSettings settings,
            TerminologyProvider terminologyProvider,
            Map<String, DataProvider> dataProviders) {

        var modelManager =
                settings.getModelCache() != null ? new ModelManager(settings.getModelCache()) : new ModelManager();

        var libraryManager = new LibraryManager(
                modelManager, settings.getCqlOptions().getCqlCompilerOptions(), settings.getLibraryCache());

        registerLibrarySourceProviders(settings, libraryManager, repository);
        registerModelInfoProviders(settings, modelManager, repository);
        registerNpmSupport(settings, libraryManager, modelManager);

        // Manually registering Using CQL and US CQL Common namespace for now
        libraryManager
                .getNamespaceManager()
                .ensureNamespaceRegistered(new NamespaceInfo("hl7.fhir.uv.cql", "http://hl7.org/fhir/uv/cql"));
        libraryManager
                .getNamespaceManager()
                .ensureNamespaceRegistered(new NamespaceInfo("hl7.fhir.us.cql", "http://hl7.org/fhir/us/cql"));

        return new Environment(libraryManager, dataProviders, terminologyProvider);
    }

    private static void registerModelInfoProviders(
            EvaluationSettings settings, ModelManager manager, IRepository repository) {
        var loader = manager.getModelInfoLoader();

        // TODO: Add a 'useEmbeddedModelInfo' setting?
        // TODO: Add support for providers in the settings?

        loader.registerModelInfoProvider(buildModelInfo(repository));
    }

    private static void registerLibrarySourceProviders(
            EvaluationSettings settings, LibraryManager manager, IRepository repository) {
        var loader = manager.getLibrarySourceLoader();
        loader.clearProviders();

        for (var s : settings.getLibrarySourceProviders()) {
            loader.registerProvider(s);
        }

        if (settings.getCqlOptions().useEmbeddedLibraries()) {
            loader.registerProvider(new FhirLibrarySourceProvider());
        }

        loader.registerProvider(buildLibrarySource(repository));
    }

    private static void registerNpmSupport(
            EvaluationSettings settings, LibraryManager libraryManager, ModelManager modelManager) {
        var npmProcessor = settings.getNpmProcessor();
        if (npmProcessor == null || npmProcessor.getIgContext() == null || npmProcessor.getPackageManager() == null) {
            return;
        }

        ILibraryReader reader = new org.cqframework.fhir.npm.LibraryLoader(
                npmProcessor.getIgContext().getFhirVersion());
        LoggerAdapter adapter = new LoggerAdapter(logger);
        libraryManager
                .getLibrarySourceLoader()
                .registerProvider(new NpmLibrarySourceProvider(
                        npmProcessor.getPackageManager().getNpmList(), reader, adapter));
        modelManager
                .getModelInfoLoader()
                .registerModelInfoProvider(new NpmModelInfoProvider(
                        npmProcessor.getPackageManager().getNpmList(), reader, adapter));

        // TODO: This is a workaround for: a) multiple packages with the same package id will be in the dependency
        // list, and b) there are packages with different package ids but the same base canonical (e.g.
        // fhir.r4.examples has the same base canonical as fhir.r4)
        // NOTE: Using ensureNamespaceRegistered works around a but not b
        Set<String> keys = new HashSet<String>();
        Set<String> uris = new HashSet<String>();
        for (var n : npmProcessor.getNamespaces()) {
            if (!keys.contains(n.getName()) && !uris.contains(n.getUri())) {
                libraryManager.getNamespaceManager().addNamespace(n);
                keys.add(n.getName());
                uris.add(n.getUri());
            }
        }
    }

    private static ModelInfoProvider buildModelInfo(IRepository repository) {
        var adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        return new RepositoryFhirModelInfoProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    private static LibrarySourceProvider buildLibrarySource(IRepository repository) {
        var adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
    }

    private static Map<String, DataProvider> buildDataProviders(
            IRepository repository,
            IBaseBundle additionalData,
            TerminologyProvider terminologyProvider,
            RetrieveSettings retrieveSettings) {
        Map<String, DataProvider> dataProviders = new HashMap<>();

        var providers = new ArrayList<RetrieveProvider>();
        var modelResolver = FhirModelResolverCache.resolverForVersion(
                repository.fhirContext().getVersion().getVersion());

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

    private static CqlEngine createEngine(Environment environment, EvaluationSettings settings) {
        var engine = new CqlEngine(
                environment, settings.getCqlOptions().getCqlEngineOptions().getOptions());
        if (settings.getCqlOptions().getCqlEngineOptions().isDebugLoggingEnabled()) {
            var map = new DebugMap();
            map.setLoggingEnabled(true);
            engine.getState().setDebugMap(map);
        }

        return engine;
    }

    public static CqlFhirParametersConverter getCqlFhirParametersConverter(FhirContext fhirContext) {
        var fhirTypeConverter =
                new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        return new CqlFhirParametersConverter(
                fhirContext, IAdapterFactory.forFhirContext(fhirContext), fhirTypeConverter);
    }

    public static boolean testUncovered(FhirContext fhirContext, String resource) {
        try {
            fhirContext.newJsonParser().parseResource(resource);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
