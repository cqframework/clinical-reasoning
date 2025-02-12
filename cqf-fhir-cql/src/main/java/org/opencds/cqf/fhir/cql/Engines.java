package org.opencds.cqf.fhir.cql;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.xml.bind.JAXB;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.LibrarySourceProvider;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider;
import org.cqframework.fhir.npm.ILibraryReader;
import org.cqframework.fhir.npm.NpmLibrarySourceProvider;
import org.cqframework.fhir.npm.NpmModelInfoProvider;
import org.cqframework.fhir.utilities.LoggerAdapter;
import org.hl7.cql.model.ModelIdentifier;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.elm_modelinfo.r1.ModelInfo;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.execution.CqlEngine;
import org.opencds.cqf.cql.engine.execution.Environment;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider;
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

public class Engines {

    private static Logger logger = LoggerFactory.getLogger(Engines.class);

    private Engines() {}

    public static CqlEngine forRepository(Repository repository) {
        return forRepository(repository, EvaluationSettings.getDefault());
    }

    public static CqlEngine forRepository(Repository repository, EvaluationSettings settings) {
        return forRepository(repository, settings, null, null);
    }

    public static CqlEngine forRepository(
            Repository repository,
            EvaluationSettings settings,
            IBaseBundle additionalData,
            NpmResourceHolder npmResourceHolder) {
        checkNotNull(settings);
        checkNotNull(repository);

        var terminologyProvider = new RepositoryTerminologyProvider(
                repository, settings.getValueSetCache(), settings.getTerminologySettings());
        var dataProviders =
                buildDataProviders(repository, additionalData, terminologyProvider, settings.getRetrieveSettings());
        var environment = buildEnvironment(repository, settings, terminologyProvider, dataProviders, npmResourceHolder);
        return createEngine(environment, settings);
    }

    private static Environment buildEnvironment(
            Repository repository,
            EvaluationSettings settings,
            TerminologyProvider terminologyProvider,
            Map<String, DataProvider> dataProviders,
            NpmResourceHolder npmResourceHolder) {

        var modelManager =
                settings.getModelCache() != null ? new ModelManager(settings.getModelCache()) : new ModelManager();

        var libraryManager = new LibraryManager(
                modelManager, settings.getCqlOptions().getCqlCompilerOptions(), settings.getLibraryCache());

        registerLibrarySourceProviders(settings, libraryManager, repository);
        registerNpmSupport(settings, libraryManager, modelManager);
        registerNpmResourceHolderGetter(libraryManager, modelManager, npmResourceHolder);

        return new Environment(libraryManager, dataProviders, terminologyProvider);
    }

    private static void registerNpmResourceHolderGetter(
            LibraryManager libraryManager, ModelManager modelManager, NpmResourceHolder npmResourceHolder) {

        var loader = libraryManager.getLibrarySourceLoader();
        // LUKETODO:  hwo to handle this?
        // LUKETODO:  only the main Library at this point, no need for multiples
        var optMainLibrary = npmResourceHolder.getOptMainLibrary();

        // LUKETODO:  figure out how to properly derive the FHIR version later
        var reader = new org.cqframework.fhir.npm.LibraryLoader(FhirVersionEnum.R4.getFhirVersionString());

        // LUKETODO:  if we have the exact match for the version, then pass it down

        //        byte[] getLibrarySource(VersionedIdentifier identifier) {
        //            var url = toUrl(identifier); // TODO
        //            var results = npmSearch.byUrl(url);
        //            var lib = latestVersion(results); // pass in "latest"
        //
        //            for (var a : lib.attachments) {
        //                if (a.contentType == "text/cql") {
        //                    return a.data;
        //                }
        //            }
        //
        //            return null;
        //        final String url = toUrl(versionedIdentifier);
        //        }

        loader.registerProvider(versionedIdentifier -> {
            if (optMainLibrary.isEmpty()) {
                return null;
            }

            final Library library = optMainLibrary.get();
            if (!doesLibraryMatch(versionedIdentifier, library)) {
                return null;
            }

            final List<Attachment> content = library.getContent();

            final Optional<Attachment> optCqlData = content.stream()
                    .filter(c -> c.getContentType().equals("text/cql"))
                    .findFirst();

            if (optCqlData.isEmpty()) {
                return null;
            }

            final Attachment attachment = optCqlData.get();

            return new ByteArrayInputStream(attachment.getData());
        });

        modelManager.getModelInfoLoader().registerModelInfoProvider(modelIdentifier -> {
            if (optMainLibrary.isEmpty()) {
                return null;
            }

            final Library library = optMainLibrary.get();

            if (!doesLibraryMatch(modelIdentifier, library)) {
                return null;
            }

            final List<Attachment> content = library.getContent();

            final Optional<Attachment> optCqlData = content.stream()
                    .filter(c -> c.getContentType().equals("application/xml"))
                    .findFirst();

            if (optCqlData.isEmpty()) {
                return null;
            }

            final Attachment attachment = optCqlData.get();

            final InputStream inputStream = new ByteArrayInputStream(attachment.getData());

            return JAXB.unmarshal(inputStream, ModelInfo.class);
        });
    }

    private static String toUrl(VersionedIdentifier versionedIdentifier) {
        //        org.hl7.fhir
        //
        //        {https://hl7.org/fhir}/Library/{id}
        // org.hl7.fhir....
        // LUKETODO:  convert sytem to URL

        // org.hl7.fhir  // from CQL  Use NamespaceManager and NamespaceInfo to conver
        return "https://" + versionedIdentifier.getSystem() + "/Library/" + versionedIdentifier.getId();
    }

    private static boolean doesLibraryMatch(VersionedIdentifier versionedIdentifier, Library libraryCandidate) {
        // LUKETODO:  this doesn't work

        if (versionedIdentifier.getId().equals(libraryCandidate.getIdPart())) {
            final Optional<Attachment> optCqlData = libraryCandidate.getContent().stream()
                    .filter(content -> content.getContentType().equals("text/cql"))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static boolean doesLibraryMatch(ModelIdentifier modelIdentifier, Library libraryCandidate) {
        if (modelIdentifier.getId().equals(libraryCandidate.getIdPart())) {
            final Optional<Attachment> optCqlData = libraryCandidate.getContent().stream()
                    .filter(content -> content.getContentType().equals("text/cql"))
                    .findFirst();

            if (optCqlData.isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static void registerLibrarySourceProviders(
            EvaluationSettings settings, LibraryManager manager, Repository repository) {
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

    private static LibrarySourceProvider buildLibrarySource(Repository repository) {
        var adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        return new RepositoryFhirLibrarySourceProvider(
                repository, adapterFactory, new LibraryVersionSelector(adapterFactory));
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
            map.setIsLoggingEnabled(true);
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
}
