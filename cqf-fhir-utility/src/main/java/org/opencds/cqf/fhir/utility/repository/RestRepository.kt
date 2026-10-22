package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.gclient.*
import ca.uhn.fhir.util.ParametersUtil
import com.google.common.collect.Multimap
import java.util.*
import java.util.function.Consumer
import org.hl7.fhir.instance.model.api.*

class RestRepository(protected val client: IGenericClient) : IRepository {
    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>?,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        val op = this.client.read().resource<T?>(resourceType).withId(id)
        return this.addHeaders<IReadExecutable<T?>?>(op, headers)!!.execute()
    }

    override fun <T : IBaseResource> create(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op = this.client.create().resource(resource)
        return this.addHeaders<ICreateTyped?>(op, headers)!!.execute()
    }

    override fun <I : IIdType, P : IBaseParameters> patch(
        id: I?,
        patchParameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op = this.client.patch().withFhirPatch(patchParameters).withId(id)
        return this.addHeaders<IPatchExecutable?>(op, headers)!!.execute()
    }

    override fun <T : IBaseResource> update(
        resource: T?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op = this.client.update().resource(resource).withId(resource!!.getIdElement())
        return this.addHeaders<IUpdateExecutable?>(op, headers)!!.execute()
    }

    override fun <T : IBaseResource, I : IIdType> delete(
        resourceType: Class<T>?,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op = this.client.delete().resourceById(id)
        return this.addHeaders<IDeleteTyped?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>?,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val params = HashMap<String?, MutableList<IQueryParameterType?>?>()
        if (searchParameters != null) {
            for (key in searchParameters.keySet()) {
                val flattenLists =
                    searchParameters
                        .get(key)
                        .stream()
                        .flatMap<IQueryParameterType?> { obj: MutableList<IQueryParameterType?>? ->
                            obj!!.stream()
                        }
                        .toList()

                params.put(key, flattenLists)
            }
            searchParameters
                .entries()
                .forEach(
                    Consumer {
                        p: MutableMap.MutableEntry<String?, MutableList<IQueryParameterType?>?>? ->
                        params.put(p!!.key, p.value)
                    }
                )
        }
        return search(bundleType, resourceType, params, mutableMapOf<String?, String?>())
    }

    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>?,
        searchParameters: MutableMap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        var op =
            this.client
                .search<IBaseBundle?>()
                .forResource(resourceType)
                .returnBundle<B?>(bundleType)
        if (searchParameters != null) {
            op = op.where(searchParameters)
        }

        if (headers != null) {
            for (entry in headers.entries) {
                op = op.withAdditionalHeader(entry.key, entry.value)
            }
        }

        return this.addHeaders<IQuery<B?>?>(op, headers)!!.execute()
    }

    override fun <C : IBaseConformance> capabilities(
        resourceType: Class<C>?,
        headers: MutableMap<String?, String?>?,
    ): C? {
        val op = this.client.capabilities().ofType<C?>(resourceType)
        return this.addHeaders<IFetchConformanceTyped<C?>?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle?> transaction(
        transaction: B?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val op = this.client.transaction().withBundle<B?>(transaction)
        return this.addHeaders<ITransactionTyped<B?>?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle> link(
        bundleType: Class<B>?,
        url: String?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val op = this.client.loadPage().byUrl(url).andReturnBundle<B?>(bundleType)
        return this.addHeaders<IGetPageTyped<B?>?>(op, headers)!!.execute()
    }

    override fun <R : IBaseResource, P : IBaseParameters> invoke(
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        val op =
            this.client
                .operation()
                .onServer()
                .named(name)
                .withParameters<P?>(parameters)
                .returnResourceType<R?>(returnType)
        return this.addHeaders<IOperationUntypedWithInput<R?>?>(op, headers)!!.execute()
    }

    override fun <P : IBaseParameters> invoke(
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op =
            this.client
                .operation()
                .onServer()
                .named(name)
                .withParameters<P?>(parameters)
                .returnMethodOutcome()
        return this.addHeaders<IOperationUntypedWithInput<MethodOutcome?>?>(op, headers)!!.execute()
    }

    override fun <R : IBaseResource, P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        val op =
            this.client
                .operation()
                .onType(resourceType)
                .named(name)
                .withParameters<P?>(parameters)
                .returnResourceType<R?>(returnType)
        return this.addHeaders<IOperationUntypedWithInput<R?>?>(op, headers)!!.execute()
    }

    override fun <P : IBaseParameters, T : IBaseResource> invoke(
        resourceType: Class<T>?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op =
            this.client
                .operation()
                .onType(resourceType)
                .named(name)
                .withParameters<P?>(parameters)
                .returnMethodOutcome()
        return this.addHeaders<IOperationUntypedWithInput<MethodOutcome?>?>(op, headers)!!.execute()
    }

    override fun <R : IBaseResource, P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        val op =
            this.client
                .operation()
                .onInstance(id)
                .named(name)
                .withParameters<P?>(parameters)
                .returnResourceType<R?>(returnType)
        return this.addHeaders<IOperationUntypedWithInput<R?>?>(op, headers)!!.execute()
    }

    override fun <P : IBaseParameters, I : IIdType> invoke(
        id: I?,
        name: String?,
        parameters: P?,
        headers: MutableMap<String?, String?>?,
    ): MethodOutcome? {
        val op =
            this.client
                .operation()
                .onInstance(id)
                .named(name)
                .withParameters<P?>(parameters)
                .returnMethodOutcome()
        return this.addHeaders<IOperationUntypedWithInput<MethodOutcome?>?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle, P : IBaseParameters> history(
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val op = this.client.history().onServer().returnBundle<B?>(returnType)
        this.addHistoryParams<IBaseBundle, P>(null, parameters)
        return this.addHeaders<IHistoryTyped<B?>?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle, P : IBaseParameters, T : IBaseResource> history(
        resourceType: Class<T>?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val op = this.client.history().onType(resourceType).returnBundle<B?>(returnType)
        this.addHistoryParams<IBaseBundle, P>(null, parameters)
        return this.addHeaders<IHistoryTyped<B?>?>(op, headers)!!.execute()
    }

    override fun <B : IBaseBundle, P : IBaseParameters, I : IIdType> history(
        id: I?,
        parameters: P?,
        returnType: Class<B>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        val op = this.client.history().onInstance(id).returnBundle<B?>(returnType)
        this.addHistoryParams<IBaseBundle, P>(null, parameters)
        return this.addHeaders<IHistoryTyped<B?>?>(op, headers)!!.execute()
    }

    override fun fhirContext(): FhirContext {
        return this.client.getFhirContext()
    }

    protected fun <B : IBaseBundle, P : IBaseParameters> addHistoryParams(
        operation: IHistoryTyped<B?>?,
        parameters: P?,
    ) {
        val ctx = this.client.getFhirContext()
        val count = ParametersUtil.getNamedParameterValuesAsInteger(ctx, parameters, "_count")
        if (count != null && !count.isEmpty()) {
            operation!!.count(count.get(0))
        }

        // TODO: Figure out how to handle date ranges for the _at parameter
        val since = ParametersUtil.getNamedParameter(ctx, parameters, "_since")
        if (since.isPresent()) {
            operation!!.since(since.get() as IPrimitiveType<Date?>)
        }
    }

    protected fun <T : IClientExecutable<T?, *>?> addHeaders(
        op: T?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        var op = op
        if (headers != null) {
            for (entry in headers.entries) {
                op = op!!.withAdditionalHeader(entry.key, entry.value)
            }
        }

        return op
    }
}
