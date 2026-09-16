package org.opencds.cqf.fhir.cql.cql2elm.content

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import org.hl7.elm.r1.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.cql.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.iterable.BundleIterable
import org.opencds.cqf.fhir.utility.search.Searches

class RepositoryFhirLibrarySourceProvider(
    protected val repository: IRepository,
    adapterFactory: IAdapterFactory,
    private val libraryVersionSelector: LibraryVersionSelector,
) : BaseFhirLibrarySourceProvider(adapterFactory) {
    protected val fhirContext: FhirContext = repository.fhirContext()

    override fun getLibrary(libraryIdentifier: VersionedIdentifier): IBaseResource? {
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
}
