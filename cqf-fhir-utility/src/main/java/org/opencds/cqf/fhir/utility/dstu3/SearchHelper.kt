package org.opencds.cqf.fhir.utility.dstu3

import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import java.util.function.Consumer
import org.hl7.fhir.dstu3.model.Bundle
import org.hl7.fhir.dstu3.model.Resource
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IPrimitiveType
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.search.Searches

object SearchHelper {
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> searchRepositoryByCanonical(
        repository: IRepository,
        canonical: CanonicalType?,
    ): Resource? {
        if (canonical == null) {
            return null
        }
        val url = Canonicals.getUrl(canonical)
        val version = Canonicals.getVersion(canonical)
        val resourceType =
            repository
                .fhirContext()
                .getResourceDefinition(Canonicals.getResourceType(canonical))
                .implementingClass

        val searchParams =
            if (version == null) Searches.byUrl(url) else Searches.byUrlAndVersion(url, version)
        val searchResult = repository.search(Bundle::class.java, resourceType, searchParams)
        if (!searchResult.hasEntry()) {
            throw FHIRException(
                "No resource of type ${resourceType.simpleName} found for url: $url|$version"
            )
        }

        return searchResult.getEntryFirstRep().getResource()
    }

    fun <T : IBaseResource?> searchRepositoryWithPaging(
        repository: IRepository,
        resourceType: Class<T>?,
        searchParameters: MutableMap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): Bundle {
        val result =
            repository.search<Bundle, T>(
                Bundle::class.java,
                resourceType,
                searchParameters,
                headers,
            )
        val next = result.getLink(IBaseBundle.LINK_NEXT)
        if (next != null) {
            getNextPage(repository, result, next.getUrl())
        }

        return result
    }

    private fun getNextPage(repository: IRepository, bundle: Bundle, nextUrl: String?) {
        val nextBundle = repository.link(Bundle::class.java, nextUrl)
        nextBundle
            .getEntry()
            .forEach(Consumer { t: Bundle.BundleEntryComponent? -> bundle.addEntry(t) })
        val next = nextBundle.getLink(IBaseBundle.LINK_NEXT)
        if (next != null) {
            getNextPage(repository, bundle, next.getUrl())
        }
    }
}
