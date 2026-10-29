package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.util.BundleBuilder
import ca.uhn.fhir.util.bundle.BundleEntryParts
import com.google.common.collect.Multimap
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Stream
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.iterable.BundleIterator
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// wip
class FederatedRepository(private val local: IRepository, vararg repositories: IRepository?) :
    IRepository {
    private val repositoryList: MutableList<IRepository>

    init {
        repositoryList = Arrays.asList<IRepository?>(*repositories)
    }

    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>?,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        // Check local first, then go through the list if nothing is found

        var result: T? = null
        try {
            result = local.read<T?, I?>(resourceType, id, headers)
        } catch (e: Exception) {
            // TODO: handle exception
        }

        if (result == null) {
            for (repository in repositoryList) {
                try {
                    result = repository.read<T?, I?>(resourceType, id, headers)
                    if (result != null) {
                        break
                    }
                } catch (e: Exception) {
                    // TODO: handle exception
                }
            }
        }

        if (result == null) {
            throw ResourceNotFoundException("No resource found with id: ${id!!.value}")
        }

        return result
    }

    override fun <T : IBaseResource> create(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <I : IIdType, P : IBaseParameters> patch(
        id: I?,
        patchParameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <T : IBaseResource> update(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <T : IBaseResource, I : IIdType> delete(
        resourceType: Class<T>?,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    private fun <B : IBaseBundle, T : IBaseResource> conductSearch(
        repository: IRepository,
        bundleType: Class<B>?,
        resourceType: Class<T>?,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): MutableList<T?> {
        val results = mutableListOf<T?>()
        val bundle = repository.search(bundleType, resourceType, searchParameters, headers)
        val iterator = BundleIterator(repository, bundle)
        iterator.forEachRemaining(
            Consumer { b: BundleEntryParts? -> results.add(b!!.getResource() as T?) }
        )
        return results
    }

    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>?,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        // Search all repositories and return combined results.
        val builder = BundleBuilder(fhirContext())

        val futureList: MutableList<CompletableFuture<MutableList<T?>?>?> =
            ArrayList<CompletableFuture<MutableList<T?>?>?>()
        futureList.add(
            CompletableFuture.supplyAsync<MutableList<T?>?>(
                Supplier {
                    conductSearch(local, bundleType, resourceType, searchParameters, headers)
                }
            )
        )
        this.repositoryList.forEach(
            Consumer { r: IRepository? ->
                futureList.add(
                    CompletableFuture.supplyAsync<MutableList<T?>?>(
                        Supplier {
                            conductSearch(r!!, bundleType, resourceType, searchParameters, headers)
                        }
                    )
                )
            }
        )

        futureList
            .stream()
            .map<MutableList<T?>?> { obj: CompletableFuture<MutableList<T?>?>? -> obj!!.join() }
            .flatMap<T?> { obj: MutableList<T?>? -> obj!!.stream() }
            .forEach { theResource: T? -> builder.addCollectionEntry(theResource) }
        builder.setType("searchset")

        return builder.getBundleTyped<B?>()
    }

    override fun <B : IBaseBundle> link(
        bundleType: Class<B>?,
        url: String?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return Stream.concat<IRepository?>(Stream.of<IRepository?>(local), repositoryList.stream())
            .map<Optional<B>?> { repo -> tryLink(repo!!, bundleType, url, headers) }
            .flatMap<B?> { obj: Optional<B>? -> obj!!.stream() }
            .filter { bundle: B? -> this.hasResourceEntries(bundle!!) }
            .findFirst()
            .orElse(null)
    }

    private fun <B : IBaseBundle> tryLink(
        repo: IRepository,
        type: Class<B>?,
        url: String?,
        headers: MutableMap<String?, String?>?,
    ): Optional<B> {
        try {
            return Optional.ofNullable(repo.link(type, url, headers))
        } catch (e: Exception) {
            // swallow and try next
            logger.debug(
                "Encountered error attempting to fetch link from repository of type {}: {}",
                repo.javaClass.getName(),
                e.message,
            )
            return Optional.empty()
        }
    }

    private fun hasResourceEntries(bundle: IBaseBundle): Boolean {
        val bundleFactory =
            FhirContext.forCached(bundle.getStructureFhirVersionEnum()).newBundleFactory()
        bundleFactory.initializeWithBundleResource(bundle)
        return !bundleFactory.toListOfResources().isEmpty()
    }

    override fun <C : IBaseConformance> capabilities(
        resourceType: Class<C>?,
        headers: MutableMap<String?, String?>?,
    ): C? {
        return null
    }

    override fun <B : IBaseBundle> transaction(
        transaction: B?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return null
    }

    override fun <R : IBaseResource, P : IBaseParameters> invoke(
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        return null
    }

    override fun <P : IBaseParameters> invoke(
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <R : IBaseResource, P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        return null
    }

    override fun <P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <R : IBaseResource, P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        return null
    }

    override fun <P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        return null
    }

    override fun <B : IBaseBundle, P : IBaseParameters> history(
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return null
    }

    override fun <B : IBaseBundle, P : IBaseParameters, T : IBaseResource> history(
        resourceType: Class<T>?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return null
    }

    override fun <B : IBaseBundle, P : IBaseParameters, I : IIdType> history(
        id: I?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return null
    }

    override fun fhirContext(): FhirContext {
        return local.fhirContext()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(FederatedRepository::class.java)
    }
}
