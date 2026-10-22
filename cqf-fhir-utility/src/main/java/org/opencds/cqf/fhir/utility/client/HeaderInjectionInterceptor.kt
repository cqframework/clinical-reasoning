package org.opencds.cqf.fhir.utility.client

import ca.uhn.fhir.rest.client.api.IClientInterceptor
import ca.uhn.fhir.rest.client.api.IHttpRequest
import ca.uhn.fhir.rest.client.api.IHttpResponse
import java.io.IOException

class HeaderInjectionInterceptor : IClientInterceptor {
    private val headers: MutableMap<String?, String?>

    /**
     * Instantiates a new header injection interception.
     *
     * @param headerKey the header key
     * @param headerValue the header value
     */
    constructor(headerKey: String?, headerValue: String?) : super() {
        this.headers = mutableMapOf()
        this.headers[headerKey] = headerValue
    }

    /**
     * Instantiates a new header injection interception.
     *
     * @param headers the headers
     */
    constructor(headers: MutableMap<String?, String?>) : super() {
        this.headers = headers
    }

    override fun interceptRequest(request: IHttpRequest) {
        for (entry in this.headers.entries) {
            request.addHeader(entry.key, entry.value)
        }
    }

    @Throws(IOException::class)
    override fun interceptResponse(response: IHttpResponse?) {
        // nothing
    }
}
