package org.opencds.cqf.fhir.cql.cql2elm.content

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.hl7.cql.model.ModelIdentifier
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.elm_modelinfo.r1.ModelInfo
import org.hl7.elm_modelinfo.r1.serializing.parseModelInfoXml
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.iterable.BundleIterable
import org.opencds.cqf.fhir.utility.search.Searches
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryFhirModelInfoProvider(
    protected val repository: IRepository,
    adapterFactory: IAdapterFactory,
    private val libraryVersionSelector: LibraryVersionSelector,
) : BaseFhirModelInfoProvider(adapterFactory) {
    val fhirContext: FhirContext = repository.fhirContext()

    override fun load(modelIdentifier: ModelIdentifier): ModelInfo? {
        val `is` = getModelInfoContent(modelIdentifier, ModelInfoContentType.XML)
        if (`is` == null) {
            logger.error("Unable to locate model info content for {}", modelIdentifier.id)
            return null
        }

        try {
            return parseModelInfoXml(`is`.asSource().buffered())
        } catch (e: Exception) {
            logger.error(
                "Error encountered while loading model info for {}: {}",
                modelIdentifier.id,
                e.message,
            )
            return null
        }
    }

    public override fun getLibrary(modelIdentifier: ModelIdentifier): IBaseResource? {
        val libraryIdentifier = toLibraryIdentifier(modelIdentifier)

        // TODO: Support lookup by URL...
        @Suppress("UNCHECKED_CAST")
        val bt =
            this.fhirContext.getResourceDefinition("Bundle").implementingClass
                as Class<IBaseBundle?>?
        val lt = this.fhirContext.getResourceDefinition("Library").implementingClass

        val libs =
            repository.search(
                bt,
                lt,
                Searches.byNameAndVersion(libraryIdentifier.id, libraryIdentifier.version),
            )

        val iter = BundleIterable<IBaseBundle?>(repository, libs).iterator()

        if (!iter.hasNext()) {
            return null
        }

        val libraries = mutableListOf<IBaseResource?>()
        iter.forEachRemaining { x -> libraries.add(x!!.resource) }

        return this.libraryVersionSelector.select(libraryIdentifier, libraries)
    }

    protected fun toLibraryIdentifier(modelIdentifier: ModelIdentifier): VersionedIdentifier {
        return VersionedIdentifier()
            .withSystem(modelIdentifier.system)
            .withId(modelIdentifier.id)
            .withVersion(modelIdentifier.version)
    }

    companion object {
        private val logger: Logger =
            LoggerFactory.getLogger(RepositoryFhirModelInfoProvider::class.java)
    }
}
