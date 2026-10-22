package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.repository.IRepository
import ca.uhn.fhir.rest.client.api.IGenericClient
import kotlin.IllegalArgumentException
import org.apache.commons.lang3.NotImplementedException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.client.Clients
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcher
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherDSTU3
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR4
import org.opencds.cqf.fhir.utility.matcher.ResourceMatcherR5

object Repositories {
    fun proxy(
        data: IRepository,
        content: IRepository?,
        terminology: IRepository?,
    ): ProxyRepository {
        return ProxyRepository(data, content, terminology)
    }

    private fun createClient(fhirContext: FhirContext, endpoint: IBaseResource?): IGenericClient {
        val fhirVersion = fhirContext.getVersion().getVersion()
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 ->
                Clients.forEndpoint(fhirContext, (endpoint as org.hl7.fhir.dstu3.model.Endpoint?)!!)

            FhirVersionEnum.R4 ->
                Clients.forEndpoint(fhirContext, (endpoint as org.hl7.fhir.r4.model.Endpoint?)!!)
            FhirVersionEnum.R5 ->
                Clients.forEndpoint(fhirContext, (endpoint as org.hl7.fhir.r5.model.Endpoint?)!!)
            else ->
                throw IllegalArgumentException(
                    "unsupported FHIR version: ${fhirVersion!!.getFhirVersionString()}"
                )
        }
    }

    @JvmStatic
    fun createRestRepository(fhirContext: FhirContext, endpoint: IBaseResource?): IRepository? {
        return if (endpoint == null) null else RestRepository(createClient(fhirContext, endpoint))
    }

    @JvmStatic
    fun proxy(
        localRepository: IRepository,
        useLocalData: Boolean?,
        dataEndpoint: IBaseResource?,
        contentEndpoint: IBaseResource?,
        terminologyEndpoint: IBaseResource?,
    ): IRepository? {
        return proxy(
            localRepository,
            useLocalData,
            createRestRepository(localRepository.fhirContext(), dataEndpoint),
            createRestRepository(localRepository.fhirContext(), contentEndpoint),
            createRestRepository(localRepository.fhirContext(), terminologyEndpoint),
        )
    }

    @JvmStatic
    fun proxy(
        localRepository: IRepository,
        useServerData: Boolean?,
        dataRepository: IRepository?,
        contentRepository: IRepository?,
        terminologyRepository: IRepository?,
    ): IRepository? {
        val useLocalData = if (useServerData == null) true else useServerData
        if (
            dataRepository == null &&
                contentRepository == null &&
                terminologyRepository == null &&
                true == useLocalData
        ) {
            return localRepository
        }
        return ProxyRepository(
            localRepository,
            useLocalData,
            dataRepository,
            contentRepository,
            terminologyRepository,
        )
    }

    fun getResourceMatcher(context: FhirContext): ResourceMatcher {
        val fhirVersion = context.getVersion().getVersion()
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> ResourceMatcherDSTU3()
            FhirVersionEnum.R4 -> ResourceMatcherR4()
            FhirVersionEnum.R5 -> ResourceMatcherR5()
            else ->
                throw NotImplementedException(
                    "Resource matching is not implemented for FHIR version: ${fhirVersion!!.getFhirVersionString()}"
                )
        }
    }
}
