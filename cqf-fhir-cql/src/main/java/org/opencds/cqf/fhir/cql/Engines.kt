package org.opencds.cqf.fhir.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider
import org.cqframework.fhir.npm.ILibraryReader
import org.cqframework.fhir.npm.LibraryLoader
import org.cqframework.fhir.npm.NpmLibrarySourceProvider
import org.cqframework.fhir.npm.NpmModelInfoProvider
import org.cqframework.fhir.utilities.LoggerAdapter
import org.hl7.cql.model.ModelInfoProvider
import org.hl7.cql.model.NamespaceInfo
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.debug.DebugMap
import org.opencds.cqf.cql.engine.execution.CqlEngine
import org.opencds.cqf.cql.engine.execution.Environment
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory
import org.opencds.cqf.cql.engine.retrieve.RetrieveProvider
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirLibrarySourceProvider
import org.opencds.cqf.fhir.cql.cql2elm.content.RepositoryFhirModelInfoProvider
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter
import org.opencds.cqf.fhir.cql.engine.retrieve.FederatedDataProvider
import org.opencds.cqf.fhir.cql.engine.retrieve.RepositoryRetrieveProvider
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings
import org.opencds.cqf.fhir.cql.engine.terminology.RepositoryTerminologyProvider
import org.opencds.cqf.fhir.utility.BundleHelper
import org.opencds.cqf.fhir.utility.Constants
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache
import org.opencds.cqf.fhir.utility.repository.InMemoryFhirRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Engines {
    private val logger: Logger = LoggerFactory.getLogger(Engines::class.java)

    @JvmStatic
    @JvmOverloads
    fun forRepository(
        repository: IRepository,
        settings: EvaluationSettings = EvaluationSettings.default,
        additionalData: IBaseBundle? = null,
    ): CqlEngine {

        val terminologyProvider =
            RepositoryTerminologyProvider(
                repository,
                settings.valueSetCache,
                settings.terminologySettings,
            )
        val dataProviders =
            buildDataProviders(
                repository,
                additionalData,
                terminologyProvider,
                settings.retrieveSettings,
            )
        val environment = buildEnvironment(repository, settings, terminologyProvider, dataProviders)
        return createEngine(environment, settings)
    }

    private fun buildEnvironment(
        repository: IRepository,
        settings: EvaluationSettings,
        terminologyProvider: TerminologyProvider?,
        dataProviders: MutableMap<String?, DataProvider?>?,
    ): Environment {
        val modelManager =
            if (settings.modelCache != null) ModelManager(settings.modelCache!!) else ModelManager()

        val libraryManager =
            LibraryManager(
                modelManager,
                settings.cqlOptions.cqlCompilerOptions,
                settings.libraryCache,
            )

        registerLibrarySourceProviders(settings, libraryManager, repository)
        registerModelInfoProviders(settings, modelManager, repository)
        registerNpmSupport(settings, libraryManager, modelManager)

        // This allows for namespaces to be manually registered downstream.
        // Ideally we don't need to do this at all and can determine what namespaces are required
        // from the library.
        // This will require more work in the CQL engine to accommodate.
        settings.registeredNamespaces.forEach { (name, uri) ->
            libraryManager.namespaceManager.ensureNamespaceRegistered(NamespaceInfo(name, uri))
        }

        return Environment(libraryManager, dataProviders, terminologyProvider)
    }

    private fun registerModelInfoProviders(
        settings: EvaluationSettings?,
        manager: ModelManager,
        repository: IRepository,
    ) {
        val loader = manager.modelInfoLoader

        // TODO: Add a 'useEmbeddedModelInfo' setting?
        // TODO: Add support for providers in the settings?
        loader.registerModelInfoProvider(buildModelInfo(repository))
    }

    private fun registerLibrarySourceProviders(
        settings: EvaluationSettings,
        manager: LibraryManager,
        repository: IRepository,
    ) {
        val loader = manager.librarySourceLoader
        loader.clearProviders()

        for (s in settings.librarySourceProviders) {
            loader.registerProvider(s)
        }

        if (settings.cqlOptions.useEmbeddedLibraries()) {
            loader.registerProvider(FhirLibrarySourceProvider())
        }

        loader.registerProvider(buildLibrarySource(repository))
    }

    private fun registerNpmSupport(
        settings: EvaluationSettings,
        libraryManager: LibraryManager,
        modelManager: ModelManager,
    ) {
        val npmProcessor = settings.npmProcessor
        if (npmProcessor == null || npmProcessor.igContext == null) {
            return
        }

        val reader: ILibraryReader = LibraryLoader(npmProcessor.igContext!!.fhirVersion)
        val adapter = LoggerAdapter(logger)
        libraryManager.librarySourceLoader.registerProvider(
            NpmLibrarySourceProvider(npmProcessor.getPackageManager().npmList, reader, adapter)
        )
        modelManager.modelInfoLoader.registerModelInfoProvider(
            NpmModelInfoProvider(npmProcessor.getPackageManager().npmList, reader, adapter)
        )

        // TODO: This is a workaround for: a) multiple packages with the same package id will be in
        // the dependency list, and b) there are packages with different package ids but the same
        // base canonical (e.g. fhir.r4.examples has the same base canonical as fhir.r4)
        // NOTE: Using ensureNamespaceRegistered works around a but not b
        val keys = mutableSetOf<String?>()
        val uris = mutableSetOf<String?>()
        for (n in npmProcessor.namespaces) {
            if (!keys.contains(n.name) && !uris.contains(n.uri)) {
                libraryManager.namespaceManager.addNamespace(n)
                keys.add(n.name)
                uris.add(n.uri)
            }
        }
    }

    private fun buildModelInfo(repository: IRepository): ModelInfoProvider {
        val adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext())
        return RepositoryFhirModelInfoProvider(
            repository,
            adapterFactory,
            LibraryVersionSelector(adapterFactory),
        )
    }

    private fun buildLibrarySource(repository: IRepository): LibrarySourceProvider {
        val adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext())
        return RepositoryFhirLibrarySourceProvider(
            repository,
            adapterFactory,
            LibraryVersionSelector(adapterFactory),
        )
    }

    private fun buildDataProviders(
        repository: IRepository,
        additionalData: IBaseBundle?,
        terminologyProvider: TerminologyProvider,
        retrieveSettings: RetrieveSettings,
    ): MutableMap<String?, DataProvider?> {
        val dataProviders = mutableMapOf<String?, DataProvider?>()

        val providers = mutableListOf<RetrieveProvider>()
        val modelResolver =
            FhirModelResolverCache.resolverForVersion(repository.fhirContext().version.version)

        val retrieveProvider =
            RepositoryRetrieveProvider(repository, terminologyProvider, retrieveSettings)
        providers.add(retrieveProvider)
        if (
            additionalData != null &&
                BundleHelper.getEntry<IBaseBackboneElement?>(additionalData).isNotEmpty()
        ) {
            val bundleRepo = InMemoryFhirRepository(repository.fhirContext(), additionalData)
            val provider =
                RepositoryRetrieveProvider(bundleRepo, terminologyProvider, retrieveSettings)
            providers.add(provider)
        }

        dataProviders[Constants.FHIR_MODEL_URI] = FederatedDataProvider(modelResolver, providers)

        return dataProviders
    }

    private fun createEngine(environment: Environment, settings: EvaluationSettings): CqlEngine {
        val engineOptions = settings.cqlOptions.cqlEngineOptions
        val engine = CqlEngine(environment, engineOptions.options)

        // Precedence: explicit debugMap > isDebugLoggingEnabled > none
        if (engineOptions.debugMap != null) {
            engine.state.debugMap = engineOptions.debugMap
        } else if (engineOptions.isDebugLoggingEnabled) {
            val map = DebugMap()
            map.isLoggingEnabled = true
            engine.state.debugMap = map
        }

        return engine
    }

    @JvmStatic
    fun getCqlFhirParametersConverter(fhirContext: FhirContext): CqlFhirParametersConverter {
        val fhirTypeConverter = FhirTypeConverterFactory().create(fhirContext.version.version)
        return CqlFhirParametersConverter(
            fhirContext,
            IAdapterFactory.forFhirContext(fhirContext),
            fhirTypeConverter,
        )
    }
}
