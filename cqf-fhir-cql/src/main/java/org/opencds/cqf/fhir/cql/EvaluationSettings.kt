package org.opencds.cqf.fhir.cql

import java.util.concurrent.ConcurrentHashMap
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.cqframework.cql.cql2elm.model.CompiledLibrary
import org.cqframework.cql.cql2elm.model.Model
import org.cqframework.fhir.npm.NpmProcessor
import org.hl7.cql.model.ModelIdentifier
import org.hl7.elm.r1.VersionedIdentifier
import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings

class EvaluationSettings {
    var modelCache: MutableMap<ModelIdentifier, Model>?
    var libraryCache: MutableMap<VersionedIdentifier, CompiledLibrary>
    var valueSetCache: MutableMap<String, MutableList<Code>>
    var librarySourceProviders: MutableList<LibrarySourceProvider>

    /**
     * A map of the registered namespaces with the key being the name and the value being the uri of
     * the namespace
     */
    var registeredNamespaces: MutableMap<String, String>
        private set

    var cqlOptions: CqlOptions

    var retrieveSettings: RetrieveSettings
    var terminologySettings: TerminologySettings
    var npmProcessor: NpmProcessor?

    internal constructor() {
        this.modelCache = ConcurrentHashMap<ModelIdentifier, Model>()
        this.libraryCache = ConcurrentHashMap<VersionedIdentifier, CompiledLibrary>()
        this.valueSetCache = ConcurrentHashMap<String, MutableList<Code>>()
        this.librarySourceProviders = ArrayList()
        this.cqlOptions = CqlOptions.defaultOptions()
        this.retrieveSettings = RetrieveSettings()
        this.terminologySettings = TerminologySettings()
        this.npmProcessor = null
        this.registeredNamespaces = ConcurrentHashMap<String, String>()
    }

    /**
     * Copy constructor for EvaluationSettings
     *
     * @param settings The EvaluationSettings being copied
     */
    constructor(settings: EvaluationSettings) {
        this.modelCache = ConcurrentHashMap<ModelIdentifier, Model>(settings.modelCache)
        this.libraryCache =
            ConcurrentHashMap<VersionedIdentifier, CompiledLibrary>(settings.libraryCache)
        this.valueSetCache = ConcurrentHashMap<String, MutableList<Code>>(settings.valueSetCache)
        this.cqlOptions = settings.cqlOptions
        this.retrieveSettings = RetrieveSettings(settings.retrieveSettings)
        this.terminologySettings = TerminologySettings(settings.terminologySettings)
        this.librarySourceProviders = ArrayList(settings.librarySourceProviders)
        this.npmProcessor =
            if (settings.npmProcessor != null) NpmProcessor(settings.npmProcessor!!.igContext)
            else null
        this.registeredNamespaces = ConcurrentHashMap<String, String>(settings.registeredNamespaces)
    }

    fun withModelCache(modelCache: MutableMap<ModelIdentifier, Model>?): EvaluationSettings {
        this.modelCache = modelCache
        return this
    }

    fun withLibraryCache(
        libraryCache: MutableMap<VersionedIdentifier, CompiledLibrary>
    ): EvaluationSettings {
        this.libraryCache = libraryCache
        return this
    }

    fun withValueSetCache(
        valueSetCache: MutableMap<String, MutableList<Code>>
    ): EvaluationSettings {
        this.valueSetCache = valueSetCache
        return this
    }

    fun withCqlOptions(cqlOptions: CqlOptions): EvaluationSettings {
        this.cqlOptions = cqlOptions
        return this
    }

    fun withRetrieveSettings(retrieveSettings: RetrieveSettings): EvaluationSettings {
        this.retrieveSettings = retrieveSettings
        return this
    }

    fun withTerminologySettings(terminologySettings: TerminologySettings): EvaluationSettings {
        this.terminologySettings = terminologySettings
        return this
    }

    fun withLibrarySourceProviders(
        librarySourceProviders: MutableList<LibrarySourceProvider>
    ): EvaluationSettings {
        this.librarySourceProviders = librarySourceProviders
        return this
    }

    fun withNpmProcessor(npmProcessor: NpmProcessor?): EvaluationSettings {
        this.npmProcessor = npmProcessor
        return this
    }

    fun setRegisteredNamespaces(namespaces: MutableMap<String, String>?) {
        this.registeredNamespaces = ConcurrentHashMap<String, String>(namespaces)
    }

    fun withRegisteredNamespaces(namespaces: MutableMap<String, String>?): EvaluationSettings {
        setRegisteredNamespaces(namespaces)
        return this
    }

    fun addRegisteredNamespace(name: String, uri: String): EvaluationSettings {
        if (!registeredNamespaces.containsKey(name)) {
            registeredNamespaces[name] = uri
        }
        return this
    }

    companion object {
        @JvmStatic
        val default: EvaluationSettings
            get() = EvaluationSettings()
    }
}
