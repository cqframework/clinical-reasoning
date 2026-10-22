package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.api.MethodOutcome
import com.google.common.base.Preconditions
import com.google.common.collect.Multimap
import java.util.*
import java.util.stream.Stream
import kotlin.Exception
import kotlin.String
import org.hl7.fhir.instance.model.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ProxyRepository : IRepository {
    // One data server, one terminology server (content defaults to data)
    // One data server, one content server (terminology defaults to data)
    // One data server, one content server, one terminology server
    private val data: IRepository?
    private val content: IRepository
    private val terminology: IRepository

    constructor(
        local: IRepository,
        useLocalData: Boolean?,
        data: IRepository?,
        content: IRepository?,
        terminology: IRepository?,
    ) {

        if (data == null) {
            if (true == useLocalData) {
                this.data = local
            } else {
                this.data = InMemoryFhirRepository(local.fhirContext())
            }
        } else {
            if (true == useLocalData) {
                this.data = FederatedRepository(local, data)
            } else {
                this.data = data
            }
        }
        this.content = if (content == null) local else content
        this.terminology = if (terminology == null) local else terminology
    }

    constructor(data: IRepository, content: IRepository?, terminology: IRepository?) {
        Preconditions.checkNotNull<IRepository?>(data)

        this.data = data
        this.content = if (content == null) this.data else content
        this.terminology = if (terminology == null) this.data else terminology
    }

    override fun <T : IBaseResource, I : IIdType> read(
        resourceType: Class<T>,
        id: I?,
        headers: MutableMap<String?, String?>?,
    ): T? {
        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.read<T?, I?>(resourceType, id, headers)
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.read<T?, I?>(resourceType, id, headers)
        } else {
            return data!!.read<T?, I?>(resourceType, id, headers)
        }
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

    override fun <B : IBaseBundle, T : IBaseResource> search(
        bundleType: Class<B>?,
        resourceType: Class<T>,
        searchParameters: Multimap<String?, MutableList<IQueryParameterType?>?>?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.search<B?, T?>(bundleType, resourceType, searchParameters, headers)
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.search<B?, T?>(bundleType, resourceType, searchParameters, headers)
        } else {
            return data!!.search<B?, T?>(bundleType, resourceType, searchParameters, headers)
        }
    }

    override fun <B : IBaseBundle> link(
        bundleType: Class<B>?,
        url: String?,
        headers: MutableMap<String?, String?>?,
    ): B? {
        return Stream.of<IRepository?>(this.data, this.content, this.terminology)
            .map<Optional<B>?> { repo: IRepository? -> tryLink(repo!!, bundleType, url, headers) }
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

    private fun hasResourceEntries(bundle: IBaseBundle): kotlin.Boolean {
        val bundleFactory =
            FhirContext.forCached(bundle.getStructureFhirVersionEnum()).newBundleFactory()
        bundleFactory.initializeWithBundleResource(bundle)
        return !bundleFactory.toListOfResources().isEmpty()
    }

    override fun <C : IBaseConformance> capabilities(
        resourceType: Class<C?>?,
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
        resourceType: Class<T>,
        name: String?,
        parameters: P?,
        returnType: Class<R>?,
        headers: MutableMap<String?, String?>?,
    ): R? {
        if (isTerminologyResource(resourceType.getSimpleName())) {
            return terminology.invoke<R?, P?, T?>(
                resourceType,
                name,
                parameters,
                returnType,
                headers,
            )
        } else if (isContentResource(resourceType.getSimpleName())) {
            return content.invoke<R?, P?, T?>(resourceType, name, parameters, returnType, headers)
        } else {
            return data!!.invoke<R?, P?, T?>(resourceType, name, parameters, returnType, headers)
        }
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
        return data!!.fhirContext()
    }

    private fun isTerminologyResource(type: String?): kotlin.Boolean {
        return (terminologyResourceSet.contains(type))
    }

    private fun isContentResource(type: String?): kotlin.Boolean {
        return (contentResourceSet.contains(type))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ProxyRepository::class.java)

        private val terminologyResourceSet: MutableSet<String?> =
            HashSet<String?>(mutableListOf<String?>("ValueSet", "CodeSystem", "ConceptMap"))

        private val contentResourceSet: MutableSet<String?> =
            HashSet<String?>(
                mutableListOf<String?>(
                    "Library",
                    "Measure",
                    "PlanDefinition",
                    "StructureDefinition",
                    "ActivityDefinition",
                    "Questionnaire",
                )
            )
    }
}
