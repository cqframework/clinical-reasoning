package org.opencds.cqf.fhir.utility.client;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This file is a copy of AdditionalRequestHeadersInterceptor from the HAPI-FHIR-Client package. It
 * was brought over to remove the need to import the HAPI-FHIR-Client library due to incompatibility
 * with Android. Hopefully, HAPI creates a generic version of Interceptors that are independent of
 * the HTTP Client implementation.
 *
 * This interceptor adds arbitrary header values to requests made by the client.
 *
 * This is now also possible directly on the Fluent Client API by calling
 * {@link ca.uhn.fhir.rest.gclient.IClientExecutable#withAdditionalHeader(String, String)}
 */
public class AdditionalRequestHeadersInterceptor {
    private final Map<String, List<String>> additionalHttpHeaders;

    /**
     * Constructor
     */
    public AdditionalRequestHeadersInterceptor() {
        additionalHttpHeaders = new HashMap<>();
    }

    /**
     * Constructor
     *
     * @param headers The additional headers to add to every request
     */
    public AdditionalRequestHeadersInterceptor(Map<String, List<String>> headers) {
        this();
        if (headers != null) {
            additionalHttpHeaders.putAll(headers);
        }
    }

    /**
     * Adds the given header value. Note that {@code headerName} and {@code headerValue} cannot be
     * null.
     *
     * @param headerName the name of the header
     * @param headerValue the value to add for the header
     * @throws NullPointerException if either parameter is {@code null}
     */
    public void addHeaderValue(String headerName, String headerValue) {
        Objects.requireNonNull(headerName, "headerName cannot be null");
        Objects.requireNonNull(headerValue, "headerValue cannot be null");

        getHeaderValues(headerName).add(headerValue);
    }

    /**
     * Adds the list of header values for the given header. Note that {@code headerName} and
     * {@code headerValues} cannot be null.
     *
     * @param headerName the name of the header
     * @param headerValues the list of values to add for the header
     * @throws NullPointerException if either parameter is {@code null}
     */
    public void addAllHeaderValues(String headerName, List<String> headerValues) {
        Objects.requireNonNull(headerName, "headerName cannot be null");
        Objects.requireNonNull(headerValues, "headerValues cannot be null");

        getHeaderValues(headerName).addAll(headerValues);
    }

    /**
     * Gets the header values list for a given header. If the header doesn't have any values, an empty
     * list will be returned.
     *
     * @param headerName the name of the header
     * @return the list of values for the header
     */
    private List<String> getHeaderValues(String headerName) {
        return additionalHttpHeaders.computeIfAbsent(headerName, x -> new ArrayList<>());
    }

    /**
     * Adds the additional header values to the HTTP request.
     *
     * @param request the HTTP request
     */
    @Hook(Pointcut.CLIENT_REQUEST)
    public void interceptRequest(IHttpRequest request) {
        for (Map.Entry<String, List<String>> header : additionalHttpHeaders.entrySet()) {
            for (String headerValue : header.getValue()) {
                if (headerValue != null) {
                    request.addHeader(header.getKey(), headerValue);
                }
            }
        }
    }
}
