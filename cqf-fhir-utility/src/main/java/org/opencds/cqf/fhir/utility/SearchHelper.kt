package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.parser.DataFormatException
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Canonicals.getResourceType
import org.opencds.cqf.fhir.utility.Canonicals.getUrl
import org.opencds.cqf.fhir.utility.Canonicals.getVersion
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo
import org.opencds.cqf.fhir.utility.search.Searches

object SearchHelper {
    internal fun getBundleClass(repository: IRepository): Class<IBaseBundle> {
        @Suppress("UNCHECKED_CAST")
        return repository.fhirContext().getResourceDefinition("Bundle").implementingClass
            as Class<IBaseBundle>
    }

    /**
     * Gets a resource class
     *
     * @param repository the repository to search
     * @param resourceType String of the resource typeget
     * @return
     */
    @JvmStatic
    fun getResourceClass(repository: IRepository, resourceType: String?): Class<IBaseResource> {
        return repository.fhirContext().getResourceDefinition(resourceType).implementingClass
            as Class<IBaseResource>
    }

    /**
     * Reads a resource from the repository
     *
     * @param repository the repository to search
     * @param id IIdType of the resource
     * @return
     */
    @JvmStatic
    fun readRepository(repository: IRepository, id: IIdType): IBaseResource? {
        return repository.read(getResourceClass(repository, id.resourceType), id)
    }

    /**
     * Searches the given Repository and returns the first entry found
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> searchRepositoryByCanonical(
        repository: IRepository,
        canonical: CanonicalType,
    ): IBaseResource {
        val resourceType = getResourceType(repository, canonical)
        return SearchHelper.searchRepositoryByCanonical(repository, canonical, resourceType)
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical URLs
     * are of the form <base>/<resourceType>/<tail>
     *
     * If the URL does not conform to the convention, the cqf-resourceType extension is used to
     * determine the type of the resource, if present.
     *
     * If no extension is present, the type of the canonical is assumed to be CodeSystem, on the
     * grounds that most (if not all) non-conventional URLs are for CodeSystem uris.
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> getResourceType(
        repository: IRepository,
        canonical: CanonicalType,
    ): Class<out IBaseResource> {
        var resourceType: Class<out IBaseResource>
        try {
            val resourceTypeString = Canonicals.getResourceType(canonical)
            if (resourceTypeString.isNullOrEmpty()) {
                throw DataFormatException()
            }
            resourceType =
                repository.fhirContext().getResourceDefinition(resourceTypeString).implementingClass
        } catch (e: DataFormatException) {
            // Use the "cqf-resourceType" extension to figure this out, if it's present
            val cqfResourceTypeExt = getResourceTypeStringFromCqfResourceTypeExtension(canonical)
            if (cqfResourceTypeExt != null) {
                try {
                    resourceType =
                        repository
                            .fhirContext()
                            .getResourceDefinition(cqfResourceTypeExt)
                            .implementingClass
                } catch (e2: DataFormatException) {
                    throw UnprocessableEntityException(
                        "cqf-resourceType extension contains invalid resource type: $cqfResourceTypeExt"
                    )
                } catch (e2: NullPointerException) {
                    throw UnprocessableEntityException(
                        "cqf-resourceType extension contains invalid resource type: $cqfResourceTypeExt"
                    )
                }
            } else {
                // NOTE: This is based on the assumption that only CodeSystems don't follow the
                // canonical pattern...
                resourceType =
                    repository.fhirContext().getResourceDefinition("CodeSystem").implementingClass
            }
        }
        return resourceType
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical URLs
     * are of the form <base>/<resourceType>/<tail>
     *
     * If the URL does not conform to the convention, the cqf-resourceType extension is used to
     * determine the type of the resource, if present.
     *
     * If no extension is present, the type of the canonical is assumed to be CodeSystem, on the
     * grounds that most (if not all) non-conventional URLs are for CodeSystem uris.
     *
     * @param repository the repository to search
     * @param dependencyInfo the canonical url to search for
     * @return
     */
    @JvmStatic
    fun getResourceType(
        repository: IRepository,
        dependencyInfo: IDependencyInfo,
    ): Class<out IBaseResource> {
        var resourceType: Class<out IBaseResource>
        try {
            val resourceTypeString = getResourceType(dependencyInfo.reference)
            if (resourceTypeString.isNullOrEmpty()) {
                throw DataFormatException()
            }
            resourceType =
                repository.fhirContext().getResourceDefinition(resourceTypeString).implementingClass
        } catch (e: DataFormatException) {
            // Use the "cqf-resourceType" extension to figure this out, if it's present
            val cqfResourceTypeExt =
                getResourceTypeStringFromCqfResourceTypeExtension(dependencyInfo.getExtension())
            if (cqfResourceTypeExt != null) {
                try {
                    resourceType =
                        repository
                            .fhirContext()
                            .getResourceDefinition(cqfResourceTypeExt)
                            .implementingClass
                } catch (e2: DataFormatException) {
                    throw UnprocessableEntityException(
                        "cqf-resourceType extension contains invalid resource type: $cqfResourceTypeExt"
                    )
                } catch (e2: NullPointerException) {
                    throw UnprocessableEntityException(
                        "cqf-resourceType extension contains invalid resource type: $cqfResourceTypeExt"
                    )
                }
            } else {
                // NOTE: This is based on the assumption that only CodeSystems don't follow the
                // canonical pattern...
                resourceType =
                    repository.fhirContext().getResourceDefinition("CodeSystem").implementingClass
            }
        }
        return resourceType
    }

    private fun <
        CanonicalType : IPrimitiveType<String?>
    > getResourceTypeStringFromCqfResourceTypeExtension(canonical: CanonicalType): String? {
        return getResourceTypeStringFromCqfResourceTypeExtension(getExtensions(canonical))
    }

    private fun getResourceTypeStringFromCqfResourceTypeExtension(
        extensions: MutableList<out IBaseExtension<*, *>?>
    ): String? {
        @Suppress("UNCHECKED_CAST")
        return extensions
            .filter { ext -> ext!!.url.contains("cqf-resourceType") }
            .map { ext -> (ext!!.value as IPrimitiveType<String?>).value }
            .firstOrNull()
    }

    private fun <CanonicalType : IPrimitiveType<String?>> getExtensions(
        canonical: CanonicalType
    ): MutableList<IBaseExtension<*, *>?> {
        @Suppress("UNCHECKED_CAST")
        return when (canonical) {
            is org.hl7.fhir.dstu3.model.PrimitiveType<*> -> {
                (canonical as org.hl7.fhir.dstu3.model.PrimitiveType<String?>)
                    .extension
                    .filterIsInstance<IBaseExtension<*, *>?>()
                    .toMutableList()
            }

            is org.hl7.fhir.r4.model.PrimitiveType<*> -> {
                (canonical as org.hl7.fhir.r4.model.PrimitiveType<String?>)
                    .extension
                    .filterIsInstance<IBaseExtension<*, *>?>()
                    .toMutableList()
            }

            is org.hl7.fhir.r5.model.PrimitiveType<*> -> {
                (canonical as org.hl7.fhir.r5.model.PrimitiveType<String?>)
                    .extension
                    .filterIsInstance<IBaseExtension<*, *>?>()
                    .toMutableList()
            }

            else -> {
                throw UnprocessableEntityException(
                    "Unsupported FHIR version for canonical: " + canonical.value
                )
            }
        }
    }

    /**
     * Gets the resource type for the given canonical, based on the convention that canonical URLs
     * are of the form <base>/<resourceType>/<tail>
     *
     * If the URL does not conform to the convention, the type of the canonical is assumed to be
     * CodeSystem, on the grounds that most (if not all) non-conventional URLs are for CodeSystem
     * uris.
     *
     * @param repository
     * @param canonical
     * @return
     */
    private fun getResourceType(
        repository: IRepository,
        canonical: String,
    ): Class<out IBaseResource> {
        return try {
            repository
                .fhirContext()
                .getResourceDefinition(getResourceType(canonical))
                .implementingClass
        } catch (e: RuntimeException) {
            // Can't use the "cqf-resourceType" extension to figure this out because we just get a
            // canonical string
            // NOTE: This is based on the assumption that only CodeSystems don't follow the
            // canonical pattern...
            repository.fhirContext().getResourceDefinition("CodeSystem").implementingClass
        }
    }

    /**
     * Searches the given Repository and returns the first entry found
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return </R></String></CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>, R : IBaseResource> searchRepositoryByCanonical(
        repository: IRepository,
        canonical: CanonicalType,
        resourceType: Class<R>,
    ): IBaseResource {
        val url = Canonicals.getUrl(canonical)
        val version = Canonicals.getVersion(canonical)
        val searchParams =
            if (version == null) Searches.byUrl(url) else Searches.byUrlAndVersion(url, version)
        val searchResult = repository.search(getBundleClass(repository), resourceType, searchParams)
        val result =
            BundleHelper.getEntryResourceFirstRep(searchResult)
                ?: throw FHIRException(
                    "No resource of type ${resourceType.simpleName} found for url: $url|$version"
                )

        return result
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> searchRepositoryByCanonicalWithPaging(
        repository: IRepository,
        canonical: CanonicalType,
    ): IBaseBundle {
        val resourceType = getResourceType(repository, canonical)

        return SearchHelper.searchRepositoryByCanonicalWithPaging(
            repository,
            canonical,
            resourceType,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType>
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return </CanonicalType>
     */
    @JvmStatic
    fun <CanonicalType : IPrimitiveType<String?>> searchRepositoryByCanonicalWithPaging(
        repository: IRepository,
        canonical: String,
    ): IBaseBundle {
        val resourceType = getResourceType(repository, canonical)

        return SearchHelper.searchRepositoryByCanonicalWithPaging(
            repository,
            canonical,
            resourceType,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param additionalSearchParams search parameters to pass on to the repository
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @return
     */
    @JvmStatic
    fun searchRepositoryByCanonicalWithPagingWithParams(
        repository: IRepository,
        canonical: String,
        additionalSearchParams: Multimap<String?, MutableList<IQueryParameterType?>?>?,
    ): IBaseBundle {
        val resourceType = getResourceType(repository, canonical)
        return SearchHelper.searchRepositoryByCanonicalWithPagingWithParams(
            repository,
            canonical,
            resourceType,
            additionalSearchParams,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return </R></String></CanonicalType>
     */
    fun <
        CanonicalType : IPrimitiveType<String?>,
        R : IBaseResource,
    > searchRepositoryByCanonicalWithPaging(
        repository: IRepository,
        canonical: CanonicalType,
        resourceType: Class<R>,
    ): IBaseBundle {
        return searchRepositoryByCanonicalWithPagingWithParams(
            repository,
            canonical,
            resourceType,
            null,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <CanonicalType> an IPrimitiveType<String> type
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @param additionalSearchParams extra search parameters to search with
     * @return </R></String></CanonicalType>
     */
    fun <
        CanonicalType : IPrimitiveType<String?>,
        R : IBaseResource,
    > searchRepositoryByCanonicalWithPagingWithParams(
        repository: IRepository,
        canonical: CanonicalType,
        resourceType: Class<R>,
        additionalSearchParams: Multimap<String?, MutableList<IQueryParameterType?>?>?,
    ): IBaseBundle {
        val url = Canonicals.getUrl(canonical)
        val version = Canonicals.getVersion(canonical)
        val searchParams =
            if (version == null) Searches.byUrl(url) else Searches.byUrlAndVersion(url, version)
        if (additionalSearchParams != null) {
            searchParams.putAll(additionalSearchParams)
        }
        return searchRepositoryWithPaging(repository, resourceType, searchParams, mutableMapOf())
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @return </R>
     */
    fun <R : IBaseResource> searchRepositoryByCanonicalWithPaging(
        repository: IRepository,
        canonical: String,
        resourceType: Class<R>,
    ): IBaseBundle {
        return searchRepositoryByCanonicalWithPagingWithParams(
            repository,
            canonical,
            resourceType,
            null,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all entries
     *
     * @param <R> an IBaseResource type
     * @param repository the repository to search
     * @param canonical the canonical url to search for
     * @param resourceType the class of the IBaseResource type
     * @param additionalSearchParams extra search parameters to search with
     * @return </R>
     */
    fun <R : IBaseResource> searchRepositoryByCanonicalWithPagingWithParams(
        repository: IRepository,
        canonical: String,
        resourceType: Class<R>,
        additionalSearchParams: Multimap<String?, MutableList<IQueryParameterType?>?>?,
    ): IBaseBundle {
        val url = getUrl(canonical)
        val version = getVersion(canonical)
        val searchParams =
            if (version == null) Searches.byUrl(url) else Searches.byUrlAndVersion(url, version)
        if (additionalSearchParams != null) {
            searchParams.putAll(additionalSearchParams)
        }
        return searchRepositoryWithPaging(repository, resourceType, searchParams, mutableMapOf())
    }

    /**
     * Searches the given Repository and handles paging to return all resources found in the search
     *
     * @param <T> an IBaseResource type
     * @param <R> an IBaseBundle type
     * @param repository the repository to search
     * @param resourceType the class of the resource being searched for
     * @param searchParameters the search parameters
     * @param headers the search headers
     * @return </R></T>
     */
    @JvmStatic
    fun <T : IBaseResource, R : IBaseBundle> searchRepositoryWithPaging(
        repository: IRepository,
        resourceType: Class<T>,
        searchParameters: MutableMap<String?, MutableList<IQueryParameterType?>?>,
        headers: MutableMap<String?, String?>?,
    ): R {
        return searchRepositoryWithPaging(
            repository,
            resourceType,
            Multimaps.forMap(searchParameters),
            headers,
        )
    }

    /**
     * Searches the given Repository and handles paging to return all resources found in the search
     *
     * @param <T> an IBaseResource type
     * @param <R> an IBaseBundle type
     * @param repository the repository to search
     * @param resourceType the class of the resource being searched for
     * @param searchParameters the search parameters
     * @param headers the search headers
     * @return </R></T>
     */
    @JvmStatic
    fun <T : IBaseResource, R : IBaseBundle> searchRepositoryWithPaging(
        repository: IRepository,
        resourceType: Class<T>,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): R {
        val bundleClass = getBundleClass(repository)
        @Suppress("UNCHECKED_CAST")
        val result = repository.search(bundleClass, resourceType, searchParameters, headers) as R
        handlePaging(repository, result)

        return result
    }

    private fun handlePaging(repository: IRepository, bundle: IBaseBundle) {
        when (val fhirVersion = repository.fhirContext().version.version) {
            FhirVersionEnum.DSTU3 -> {
                val dstu3Bundle = bundle as org.hl7.fhir.dstu3.model.Bundle
                val dstu3Next = dstu3Bundle.getLink(IBaseBundle.LINK_NEXT)
                if (dstu3Next != null) {
                    getNextPageDstu3(repository, dstu3Bundle, dstu3Next.url)
                }
            }

            FhirVersionEnum.R4 -> {
                val r4Bundle = bundle as org.hl7.fhir.r4.model.Bundle
                val r4Next = r4Bundle.getLink(IBaseBundle.LINK_NEXT)
                if (r4Next != null) {
                    getNextPageR4(repository, r4Bundle, r4Next.url)
                }
            }

            FhirVersionEnum.R5 -> {
                val r5Bundle = bundle as org.hl7.fhir.r5.model.Bundle
                val r5Next = r5Bundle.getLink(IBaseBundle.LINK_NEXT)
                if (r5Next != null) {
                    getNextPageR5(repository, r5Bundle, r5Next.url)
                }
            }

            else ->
                throw IllegalArgumentException(
                    "Unsupported version of FHIR: ${fhirVersion.fhirVersionString}"
                )
        }
    }

    private fun getNextPageDstu3(
        repository: IRepository,
        bundle: org.hl7.fhir.dstu3.model.Bundle,
        nextUrl: String?,
    ) {
        val nextBundle =
            repository.link(org.hl7.fhir.dstu3.model.Bundle::class.java, nextUrl)
                as org.hl7.fhir.dstu3.model.Bundle
        nextBundle.entry.forEach { t -> bundle.addEntry(t) }
        val next = nextBundle.getLink(IBaseBundle.LINK_NEXT)
        if (next != null) {
            getNextPageDstu3(repository, bundle, next.url)
        }
    }

    private fun getNextPageR4(
        repository: IRepository,
        bundle: org.hl7.fhir.r4.model.Bundle,
        nextUrl: String?,
    ) {
        val nextBundle = repository.link(org.hl7.fhir.r4.model.Bundle::class.java, nextUrl)
        nextBundle.entry.forEach { t -> bundle.addEntry(t) }
        val next = nextBundle.getLink(IBaseBundle.LINK_NEXT)
        if (next != null) {
            getNextPageR4(repository, bundle, next.url)
        }
    }

    private fun getNextPageR5(
        repository: IRepository,
        bundle: org.hl7.fhir.r5.model.Bundle,
        nextUrl: String?,
    ) {
        val nextBundle = repository.link(org.hl7.fhir.r5.model.Bundle::class.java, nextUrl)
        nextBundle.entry.forEach { t -> bundle.addEntry(t) }
        val next = nextBundle.getLink(IBaseBundle.LINK_NEXT)
        if (next != null) {
            getNextPageR5(repository, bundle, next.url)
        }
    }
}
