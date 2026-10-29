package org.opencds.cqf.fhir.utility.client

import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Pointcut
import ca.uhn.fhir.rest.client.api.IHttpRequest

/**
 * This file is a copy of AdditionalRequestHeadersInterceptor from the HAPI-FHIR-Client package. It
 * was brought over to remove the need to import the HAPI-FHIR-Client library due to incompatibility
 * with Android. Hopefully, HAPI creates a generic version of Interceptors that are independent of
 * the HTTP Client implementation.
 *
 * This interceptor adds arbitrary header values to requests made by the client.
 *
 * This is now also possible directly on the Fluent Client API by calling
 * [ca.uhn.fhir.rest.gclient.IClientExecutable.withAdditionalHeader]
 */
class AdditionalRequestHeadersInterceptor() {
    private val additionalHttpHeaders = mutableMapOf<String?, MutableList<String?>?>()

    /**
     * Constructor
     *
     * @param headers The additional headers to add to every request
     */
    constructor(headers: MutableMap<String?, MutableList<String?>?>?) : this() {
        if (headers != null) {
            additionalHttpHeaders.putAll(headers)
        }
    }

    /**
     * Adds the given header value. Note that `headerName` and `headerValue` cannot be null.
     *
     * @param headerName the name of the header
     * @param headerValue the value to add for the header
     * @throws NullPointerException if either parameter is `null`
     */
    fun addHeaderValue(headerName: String, headerValue: String) {

        getHeaderValues(headerName)!!.add(headerValue)
    }

    /**
     * Adds the list of header values for the given header. Note that `headerName` and
     * `headerValues` cannot be null.
     *
     * @param headerName the name of the header
     * @param headerValues the list of values to add for the header
     * @throws NullPointerException if either parameter is `null`
     */
    fun addAllHeaderValues(headerName: String, headerValues: MutableList<String?>) {

        getHeaderValues(headerName)!!.addAll(headerValues)
    }

    /**
     * Gets the header values list for a given header. If the header doesn't have any values, an
     * empty list will be returned.
     *
     * @param headerName the name of the header
     * @return the list of values for the header
     */
    private fun getHeaderValues(headerName: String?): MutableList<String?>? {
        return additionalHttpHeaders.computeIfAbsent(headerName) { mutableListOf() }
    }

    /**
     * Adds the additional header values to the HTTP request.
     *
     * @param request the HTTP request
     */
    @Hook(Pointcut.CLIENT_REQUEST)
    fun interceptRequest(request: IHttpRequest) {
        for (header in additionalHttpHeaders.entries) {
            for (headerValue in header.value!!) {
                if (headerValue != null) {
                    request.addHeader(header.key, headerValue)
                }
            }
        }
    }
}
