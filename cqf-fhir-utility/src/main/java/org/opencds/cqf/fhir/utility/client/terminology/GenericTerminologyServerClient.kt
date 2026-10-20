package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import java.util.Optional
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.TerminologyCapabilities
import org.opencds.cqf.fhir.utility.Parameters.newStringPart
import org.opencds.cqf.fhir.utility.Parameters.newUriPart
import org.opencds.cqf.fhir.utility.Parameters.newUrlPart
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter.Companion.findLatestVersion
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter
import org.opencds.cqf.fhir.utility.client.Clients
import org.opencds.cqf.fhir.utility.client.ExpandRunner
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings
import org.opencds.cqf.fhir.utility.search.Searches

open class GenericTerminologyServerClient
@JvmOverloads
constructor(
    fhirContext: FhirContext,
    terminologyServerClientSettings: TerminologyServerClientSettings? = null,
) : BaseTerminologyProvider(fhirContext), ITerminologyServerClient {
    override val terminologyServerClientSettings: TerminologyServerClientSettings =
        terminologyServerClientSettings ?: TerminologyServerClientSettings.default

    override fun expand(
        valueSet: IValueSetAdapter?,
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
    ): IBaseResource? {
        requireNotNull(valueSet) { "expected non-null value for valueSet" }
        requireNotNull(endpoint) { "expected non-null value for endpoint" }
        requireNotNull(parameters) { "expected non-null value for parameters" }
        return expand(
            endpoint,
            parameters,
            valueSet.url,
            valueSet.version,
            valueSet.get().structureFhirVersionEnum,
        )
    }

    override fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        requireNotNull(endpoint) { "expected non-null value for endpoint" }
        requireNotNull(parameters) { "expected non-null value for parameters" }
        requireNotNull(fhirVersion) { "expected non-null value for fhirVersion" }
        requireNotNull(parameters.getParameter(ITerminologyServerClient.urlParamName)) {
            "expected non-null value for 'url' expansion parameter"
        }
        return expand(endpoint, parameters, null, null, fhirVersion)
    }

    override fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        url: String?,
        valueSetVersion: String?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource? {
        requireNotNull(endpoint) { "expected non-null value for endpoint" }
        requireNotNull(parameters) { "expected non-null value for parameters" }
        val fhirClient = initializeClientWithAuth(endpoint)
        if (parameters.getParameter(ITerminologyServerClient.urlParamName) == null) {
            if (url == null) {
                throw UnprocessableEntityException(
                    "No '" + ITerminologyServerClient.urlParamName + "' expansion parameter present"
                )
            }
            parameters.addParameter(
                if (fhirVersion == FhirVersionEnum.DSTU3)
                    newUriPart(fhirContext, ITerminologyServerClient.urlParamName, url)
                else newUrlPart(fhirContext, ITerminologyServerClient.urlParamName, url)
            )
        }
        if (
            parameters.getParameter(ITerminologyServerClient.versionParamName) == null &&
                valueSetVersion != null
        ) {
            parameters.addParameter(
                newStringPart(
                    fhirContext,
                    ITerminologyServerClient.versionParamName,
                    valueSetVersion,
                )
            )
        }
        return expand(fhirClient, url, parameters.get() as IBaseParameters)
    }

    override fun expand(
        fhirClient: IGenericClient?,
        url: String?,
        parameters: IBaseParameters?,
    ): IBaseResource? {
        val expandRunner =
            ExpandRunner(fhirClient!!, terminologyServerClientSettings, url!!, parameters)
        return expandRunner.expandValueSet()
    }

    override fun getR4TerminologyCapabilities(
        endpoint: IEndpointAdapter?
    ): TerminologyCapabilities? {
        val fhirClient = initializeClientWithAuth(endpoint)

        return fhirClient.fetchResourceFromUrl(
            TerminologyCapabilities::class.java,
            "/metadata?mode=terminology",
        )
    }

    override fun initializeClientWithAuth(endpoint: IEndpointAdapter?): IGenericClient {
        val fhirClient = fhirContext.newRestfulGenericClient(getAddressBase(endpoint!!.address!!))

        if (endpoint.hasHeaders()) {
            Clients.registerHeaders(fhirClient, endpoint.headers)
        }

        fhirClient.fhirContext.restfulClientFactory.socketTimeout =
            terminologyServerClientSettings.socketTimeout * 1000

        return fhirClient
    }

    override fun getCodeSystemResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        return findLatestVersion(
            initializeClientWithAuth(endpoint)
                .search<IBaseBundle>()
                .forResource(this.codeSystemClass)
                .where(Searches.toFlattenedMap(Searches.byCanonical(url)))
                .execute()
        )
    }

    override fun getLatestValueSetResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        val urlParams = Searches.toFlattenedMap(Searches.byCanonical(url))
        return findLatestVersion(
            initializeClientWithAuth(endpoint)
                .search<IBaseBundle>()
                .forResource(this.valueSetClass)
                .where(urlParams)
                .execute()
        )
    }

    override fun getValueSetResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource> {
        return findLatestVersion(
            initializeClientWithAuth(endpoint)
                .search<IBaseBundle>()
                .forResource(this.valueSetClass)
                .where(Searches.toFlattenedMap(Searches.byCanonical(url)))
                .execute()
        )
    }
}
