package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.util.BundleBuilder
import ca.uhn.fhir.util.BundleUtil
import com.google.common.base.Preconditions
import com.google.common.collect.Multimap
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.BundleHelper
import org.opencds.cqf.fhir.utility.BundleHelper.newBundle
import org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResource
import org.opencds.cqf.fhir.utility.BundleHelper.newEntryWithResponse
import org.opencds.cqf.fhir.utility.BundleHelper.newResponseWithLocation
import org.opencds.cqf.fhir.utility.Canonicals
import org.opencds.cqf.fhir.utility.Ids.newRandomId
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher
import org.opencds.cqf.fhir.utility.operation.OperationRegistry

open class InMemoryFhirRepository : IRepository {
    private val resourceMap: MutableMap<String?, MutableMap<String?, IBaseResource?>>
    private val context: FhirContext
    private val operationRegistry: OperationRegistry
    private val resourceMatcher: ResourceMatcher

    constructor(context: FhirContext) {
        this.context = context
        this.resourceMap = HashMap<String?, MutableMap<String?, IBaseResource?>>()
        this.operationRegistry = OperationRegistry()
        this.resourceMatcher = Repositories.getResourceMatcher(this.context)
    }

    constructor(context: FhirContext, bundle: IBaseBundle?) {
        this.context = context
        val resources = BundleUtil.toListOfResources(this.context, bundle)
        this.resourceMap =
            resources
                .stream()
                .collect(
                    Collectors.groupingBy(
                        Function { obj: IBaseResource? -> obj!!.fhirType() },
                        Collectors.toMap(
                            Function { r: IBaseResource? -> r!!.getIdElement().getIdPart() },
                            Function.identity<IBaseResource?>(),
                        ),
                    )
                )
        this.operationRegistry = OperationRegistry()
        this.resourceMatcher = Repositories.getResourceMatcher(this.context)
    }

    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        val resources =
            this.resourceMap.computeIfAbsent(resourceType.getSimpleName()) { x ->
                HashMap<String?, IBaseResource?>()
            }

        val resource = resources.get(id!!.idPart)

        if (resource == null) {
            throw ResourceNotFoundException(id)
        }

        return resource as T
    }

    override fun <T : IBaseResource> create(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {
        val resources =
            resourceMap.computeIfAbsent(resource!!.fhirType()) { r: String? ->
                HashMap<String?, IBaseResource?>()
            }
        var theId = newRandomId<IIdType>(context, resource.fhirType())
        while (resources.containsKey(theId.getIdPart())) {
            theId = newRandomId<IIdType>(context, resource.fhirType())
        }
        resource.setId(theId)
        val outcome = MethodOutcome(theId, true)
        resources.put(theId.getIdPart(), resource)
        return outcome
    }

    override fun <I : IIdType, P : IBaseParameters> patch(
        id: I?,
        patchParameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        throw NotImplementedOperationException("The PATCH operation is not currently supported")
    }

    override fun <T : IBaseResource> update(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {
        val resources =
            resourceMap.computeIfAbsent(resource!!.fhirType()) { r: String? ->
                HashMap<String?, IBaseResource?>()
            }
        val theId = resource.getIdElement().toUnqualifiedVersionless()
        val idPart = theId.getIdPart()
        val outcome = MethodOutcome(theId, false)
        if (!resources.containsKey(idPart)) {
            outcome.setCreated(true)
        }
        if (resource.fhirType() == "SearchParameter") {
            this.resourceMatcher.addCustomParameter(
                BundleHelper.resourceToRuntimeSearchParam(resource)
            )
        }
        resources.put(idPart, resource)

        return outcome
    }

    override fun <T : IBaseResource, I : IIdType> delete(
        resourceType: Class<T>?,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome {
        val outcome = MethodOutcome(id, false)
        val resources =
            resourceMap.computeIfAbsent(id!!.getResourceType()) { r: String? ->
                HashMap<String?, IBaseResource?>()
            }
        val keyId = id.getIdPart()
        if (resources.containsKey(keyId)) {
            outcome.setResource(resources.get(keyId))
            resources.remove(keyId)
        } else {
            throw ResourceNotFoundException("Resource not found with id " + id)
        }
        return outcome
    }

    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val builder = BundleBuilder(this.context)
        val resourceIdMap =
            resourceMap.computeIfAbsent(resourceType.simpleName) { r ->
                HashMap<String?, IBaseResource?>()
            }

        if (searchParameters == null || searchParameters.isEmpty) {
            resourceIdMap.values.forEach(
                Consumer { theResource: IBaseResource -> builder.addCollectionEntry(theResource) }
            )
            builder.setType("searchset")
            return builder.getBundle() as B?
        }

        val candidates: MutableCollection<IBaseResource?>?
        if (searchParameters.containsKey("_id")) {
            // We are consuming the _id parameter in this if statement
            val idQueries = searchParameters.get("_id")

            // The _id param can be a list of ids
            val idResources = ArrayList<IBaseResource?>(idQueries.size)
            for (idQuery in idQueries) {
                checkNotNull(idQuery)
                for (query in idQuery) {
                    if (query is TokenParam) {
                        val r = resourceIdMap.get(query.getValue())
                        if (r != null) {
                            idResources.add(r)
                        }
                    }
                }
            }

            candidates = idResources
            searchParameters.removeAll("_id")
        } else {
            candidates = resourceIdMap.values
        }

        // Apply the rest of the filters
        val keyset = searchParameters.keySet()
        for (resource in candidates) {
            var include = true
            for (paramName in keyset) {
                /*
                 * each individual entry in a list is 'or'd'.
                 * All the lists from each
                 */
                val values = searchParameters.get(paramName)
                var matches = false
                for (ors in values) {
                    matches = this.resourceMatcher.matches(paramName, ors, resource!!)
                    if (!matches) {
                        // break if no match, since each list is 'anded' with each other
                        break
                    }
                }
                include = matches

                if (!include) {
                    break
                }
            }

            if (include) {
                builder.addCollectionEntry(resource)
            }
        }

        builder.setType("searchset")
        return builder.getBundle() as B?
    }

    override fun <B : IBaseBundle> link(
        bundleType: Class<B>?,
        url: String?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        throw NotImplementedOperationException("Paging is not currently supported")
    }

    override fun <C : IBaseConformance> capabilities(
        resourceType: Class<C?>?,
        headers: MutableMap<String?, String?>?,
    ): C? {
        throw NotImplementedOperationException(
            "The capabilities interaction is not currently supported"
        )
    }

    override fun <B : IBaseBundle> transaction(
        transaction: B?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val version = transaction!!.getStructureFhirVersionEnum()

        val returnBundle = newBundle(version) as B?
        BundleHelper.getEntry<IBaseBackboneElement>(transaction)
            .forEach(
                Consumer { e: IBaseBackboneElement? ->
                    if (BundleHelper.isEntryRequestPut(version, e!!)) {
                        val outcome =
                            this.update<IBaseResource?>(BundleHelper.getEntryResource(version, e))
                        val location = outcome.getId().getValue()
                        BundleHelper.addEntry(
                            returnBundle!!,
                            newEntryWithResponse(
                                version,
                                newResponseWithLocation(version, location),
                            ),
                        )
                    } else if (BundleHelper.isEntryRequestPost(version, e)) {
                        val outcome =
                            this.create<IBaseResource?>(BundleHelper.getEntryResource(version, e))
                        val location = outcome.getId().getValue()
                        BundleHelper.addEntry(
                            returnBundle!!,
                            newEntryWithResponse(
                                version,
                                newResponseWithLocation(version, location),
                            ),
                        )
                    } else if (BundleHelper.isEntryRequestDelete(version, e)) {
                        if (BundleHelper.getEntryRequestId(version, e) != null) {
                            val resourceType =
                                Canonicals.getResourceType(
                                    BundleHelper.getEntryRequestUrl(version, e)!!
                                )
                            val resourceClass =
                                this.context
                                    .getResourceDefinition(resourceType)
                                    .getImplementingClass()
                            val res =
                                this.delete(
                                    resourceClass,
                                    BundleHelper.getEntryRequestId(version, e)!!.withResourceType(
                                        resourceType
                                    ),
                                )
                            BundleHelper.addEntry(
                                returnBundle!!,
                                newEntryWithResource(res.getResource()),
                            )
                        } else {
                            throw ResourceNotFoundException("Trying to delete an entry without id")
                        }
                    } else {
                        throw NotImplementedOperationException(
                            "Transaction stub only supports PUT, POST or DELETE"
                        )
                    }
                }
            )

        return returnBundle
    }

    override fun <R : IBaseResource, P : IBaseParameters> invoke(
        name: String,
        parameters: P?,
        returnType: Class<R>,
        headers: MutableMap<String?, String?>,
    ): R? {
        try {
            val result =
                operationRegistry
                    .buildInvocationContext(this, name!!)
                    .parameters(parameters)
                    .execute()
            return returnType!!.cast(result)
        } catch (e: BaseServerResponseException) {
            throw e
        } catch (e: Throwable) {
            throw InternalErrorException(e)
        }
    }

    override fun <P : IBaseParameters> invoke(
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        throw NotImplementedOperationException("Invoke is not currently supported")
    }

    override fun <R : IBaseResource, P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        Preconditions.checkNotNull<Class<T>?>(resourceType, "resourceType is required")
        Preconditions.checkNotNull<String?>(name, "name is required")
        Preconditions.checkNotNull<Class<R>?>(returnType, "returnType is required")
        Preconditions.checkNotNull<MutableMap<String?, String?>?>(headers, "headers are required")
        try {
            val result =
                operationRegistry
                    .buildInvocationContext(this, name!!)
                    .parameters(parameters)
                    .resourceType(resourceType)
                    .execute()
            return returnType!!.cast(result)
        } catch (e: BaseServerResponseException) {
            throw e
        } catch (e: Throwable) {
            throw InternalErrorException(e)
        }
    }

    override fun <P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        throw NotImplementedOperationException("Invoke is not currently supported")
    }

    override fun <R : IBaseResource, P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        Preconditions.checkNotNull<I?>(id, "id is required")
        Preconditions.checkArgument(
            id!!.hasResourceType(),
            "resourceType is required for id scoped operations",
        )
        Preconditions.checkNotNull<String?>(name, "name is required")
        Preconditions.checkNotNull<Class<R>?>(returnType, "returnType is required")
        Preconditions.checkNotNull<MutableMap<String?, String?>?>(headers, "headers are required")
        try {
            val result =
                operationRegistry
                    .buildInvocationContext(this, name!!)
                    .parameters(parameters)
                    .id(id)
                    .resourceType(
                        this.context
                            .getResourceDefinition(id.getResourceType())
                            .getImplementingClass()
                    )
                    .execute()
            return returnType!!.cast(result)
        } catch (e: BaseServerResponseException) {
            throw e
        } catch (e: Throwable) {
            throw InternalErrorException(e)
        }
    }

    override fun <P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        throw NotImplementedOperationException("Invoke is not currently supported")
    }

    override fun <B : IBaseBundle, P : IBaseParameters> history(
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        throw NotImplementedOperationException("The history interaction is not currently supported")
    }

    override fun <B : IBaseBundle, P : IBaseParameters, T : IBaseResource> history(
        resourceType: Class<T>?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        throw NotImplementedOperationException("The history interaction is not currently supported")
    }

    override fun <B : IBaseBundle, P : IBaseParameters, I : IIdType> history(
        id: I?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        throw NotImplementedOperationException("The history interaction is not currently supported")
    }

    override fun fhirContext(): FhirContext {
        return this.context
    }

    /**
     * Register an operation with the repository. This must be a class with at least one method
     * annotated with the @Operation annotation. The factory function is used to create an instance
     * of the operation class on a per-request basis. All methods on the operation class marked with
     * the @Operation annotation will be available for invocation.
     *
     * @param <T> the type of the operation class
     * @param clazz the operation class
     * @param factory a factory function that will create an instance of the operation class </T>
     */
    fun <T> registerOperation(clazz: Class<T>, factory: Function<IRepository?, T?>) {
        operationRegistry.register(clazz!!, factory!!)
    }
}
