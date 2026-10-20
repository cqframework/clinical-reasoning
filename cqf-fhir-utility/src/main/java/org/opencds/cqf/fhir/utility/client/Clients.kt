package org.opencds.cqf.fhir.utility.client

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.client.api.IClientInterceptor
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor
import com.google.common.base.Preconditions
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import org.apache.commons.lang3.StringUtils
import org.hl7.fhir.dstu3.model.Endpoint
import org.hl7.fhir.dstu3.model.StringType

/**
 * This class provides utility functions for creating IGenericClients and setting up authentication
 */
object Clients {
    /**
     * Creates an IGenericClient for the given url. Defaults to NEVER ServerValidationMode
     *
     * @param fhirVersionEnum the FHIR version to create a client for
     * @param url the server base url to connect to
     * @return IGenericClient for the given url
     */
    @JvmStatic
    fun forUrl(fhirVersionEnum: FhirVersionEnum, url: String): IGenericClient {

        return forUrl(FhirContext.forCached(fhirVersionEnum), url)
    }

    /**
     * Creates an IGenericClient for the given url. Defaults to NEVER ServerValidationMode
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param url the server base url to connect to
     * @return IGenericClient for the given url
     */
    @JvmStatic
    fun forUrl(fhirContext: FhirContext, url: String): IGenericClient {

        return forUrl(fhirContext, url, ServerValidationModeEnum.NEVER)
    }

    /**
     * Creates an IGenericClient for the given url.
     *
     * @param fhirVersionEnum the FHIR version to create a client for
     * @param url the server base url to connect to
     * @param serverValidationModeEnum the ServerValidationMode to use
     * @return IGenericClient for the given url, with the server validation mode set
     */
    @JvmStatic
    fun forUrl(
        fhirVersionEnum: FhirVersionEnum,
        url: String,
        serverValidationModeEnum: ServerValidationModeEnum,
    ): IGenericClient {

        return forUrl(FhirContext.forCached(fhirVersionEnum), url, serverValidationModeEnum)
    }

    /**
     * Creates an IGenericClient for the given url.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param url the server base url to connect to
     * @param serverValidationModeEnum the ServerValidationMode to use
     * @return IGenericClient for the given url, with the server validation mode set
     */
    @JvmStatic
    fun forUrl(
        fhirContext: FhirContext,
        url: String,
        serverValidationModeEnum: ServerValidationModeEnum,
    ): IGenericClient {

        fhirContext.restfulClientFactory.serverValidationMode = serverValidationModeEnum
        return fhirContext.newRestfulGenericClient(url)
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(endpoint: Endpoint): IGenericClient {

        return forEndpoint(FhirContext.forDstu3Cached(), endpoint)
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(fhirContext: FhirContext, endpoint: Endpoint): IGenericClient {

        val client = forUrl(fhirContext, endpoint.getAddress())
        if (endpoint.hasHeader()) {
            val headerList =
                endpoint
                    .getHeader()
                    .stream()
                    .map<String?> { headerString: StringType? -> headerString!!.asStringValue() }
                    .collect(Collectors.toList())
            registerHeaders(client, headerList)
        }
        return client
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(endpoint: org.hl7.fhir.r4.model.Endpoint): IGenericClient {

        return forEndpoint(FhirContext.forR4Cached(), endpoint)
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(
        fhirContext: FhirContext,
        endpoint: org.hl7.fhir.r4.model.Endpoint,
    ): IGenericClient {

        val client = forUrl(fhirContext, endpoint.getAddress())
        if (endpoint.hasHeader()) {
            val headerList =
                endpoint
                    .getHeader()
                    .map { headerString -> headerString!!.asStringValue() }
                    .toMutableList()
            registerHeaders(client, headerList)
        }
        return client
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(endpoint: org.hl7.fhir.r5.model.Endpoint): IGenericClient {
        return forEndpoint(FhirContext.forR4Cached(), endpoint)
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    @JvmStatic
    fun forEndpoint(
        fhirContext: FhirContext,
        endpoint: org.hl7.fhir.r5.model.Endpoint,
    ): IGenericClient {

        val client = forUrl(fhirContext, endpoint.getAddress())
        if (endpoint.hasHeader()) {
            val headerList =
                endpoint
                    .getHeader()
                    .map { headerString -> headerString!!.asStringValue() }
                    .toMutableList()
            registerHeaders(client, headerList)
        }
        return client
    }

    /**
     * Registers HeaderInjectionInterceptors on a client.
     *
     * @param client the client to add headers to
     * @param headers an Array of Strings representing headers to add
     */
    @JvmStatic
    fun registerHeaders(client: IGenericClient, vararg headers: String?) {

        registerHeaders(client, mutableListOf(*headers))
    }

    /**
     * Registers HeaderInjectionInterceptors on a client
     *
     * @param client the client to add headers to
     * @param headerList a List of Strings representing headers to add
     */
    fun registerHeaders(client: IGenericClient, headerList: MutableList<String?>) {

        val headerMap = setupHeaderMap(headerList)
        for (entry in headerMap.entries) {
            val headInterceptor: IClientInterceptor =
                HeaderInjectionInterceptor(entry.key, entry.value)
            client.registerInterceptor(headInterceptor)
        }
    }

    /**
     * Registers BasicAuthInterceptors on a client. This is useful when you have a username and
     * password.
     *
     * @param client the client to register basic auth on
     * @param username the username
     * @param password the password
     */
    @JvmStatic
    fun registerBasicAuth(client: IGenericClient?, username: String?, password: String?) {
        Preconditions.checkNotNull<IGenericClient?>(client, "client is required")

        if (username != null) {
            val authInterceptor = BasicAuthInterceptor(username, password)
            client!!.registerInterceptor(authInterceptor)
        }
    }

    /**
     * Registers BearerAuthInterceptors on a client. This is useful when you have a bearer token.
     *
     * @param client the client to register BearerToken authentication on
     * @param token the bearer token to register
     */
    fun registerBearerTokenAuth(client: IGenericClient?, token: String?) {
        Preconditions.checkNotNull<IGenericClient?>(client, "client is required")

        if (token != null) {
            val authInterceptor = BearerTokenAuthInterceptor(token)
            client!!.registerInterceptor(authInterceptor)
        }
    }

    @JvmStatic
    fun registerAdditionalRequestHeadersAuth(
        client: IGenericClient?,
        username: String?,
        apiKey: String?,
    ) {
        Preconditions.checkNotNull<IGenericClient?>(client, "client is required")

        if (username != null && apiKey != null) {
            val authString =
                StringUtils.join<String?>(
                    "Basic ",
                    Base64.getEncoder()
                        .encodeToString(
                            StringUtils.join<String?>(username, ":", apiKey)
                                .toByteArray(StandardCharsets.UTF_8)
                        ),
                )
            val authInterceptor = AdditionalRequestHeadersInterceptor()
            authInterceptor.addHeaderValue("Authorization", authString)
            client!!.registerInterceptor(authInterceptor)
        }
    }

    /**
     * Parses a list of headers into their constituent parts. Used to prep the headers for
     * registration with HeaderInjectionInterceptors
     *
     * @param headerList a List of Strings representing headers to create
     * @return key-value pairs of headers
     */
    fun setupHeaderMap(headerList: MutableList<String?>): MutableMap<String?, String?> {

        val headerMap = mutableMapOf<String?, String?>()
        var leftAuth: String? = null
        var rightAuth: String? = null
        if (headerList.isEmpty()) {
            leftAuth = null
            rightAuth = null
            headerMap[leftAuth] = rightAuth
        } else {
            for (header in headerList) {
                require(header!!.contains(":")) { "Endpoint header must contain \":\" ." }
                val authSplit = header.split(":")
                leftAuth = authSplit[0]
                rightAuth = authSplit[1]
                headerMap[leftAuth] = rightAuth
            }
        }
        return headerMap
    }
}
