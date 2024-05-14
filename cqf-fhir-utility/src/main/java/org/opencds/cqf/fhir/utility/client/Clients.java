package org.opencds.cqf.fhir.utility.client;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * This class provides utility functions for creating IGenericClients and setting up authentication
 */
public class Clients {

    private Clients() {}

    /**
     * Creates an IGenericClient for the given url. Defaults to NEVER ServerValidationMode
     *
     * @param fhirVersionEnum the FHIR version to create a client for
     * @param url the server base url to connect to
     * @return IGenericClient for the given url
     */
    public static IGenericClient forUrl(FhirVersionEnum fhirVersionEnum, String url) {
        checkNotNull(fhirVersionEnum);
        checkNotNull(url);

        return forUrl(FhirContext.forCached(fhirVersionEnum), url);
    }

    /**
     * Creates an IGenericClient for the given url. Defaults to NEVER ServerValidationMode
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param url the server base url to connect to
     * @return IGenericClient for the given url
     */
    public static IGenericClient forUrl(FhirContext fhirContext, String url) {
        checkNotNull(fhirContext);
        checkNotNull(url);

        return forUrl(fhirContext, url, ServerValidationModeEnum.NEVER);
    }

    /**
     * Creates an IGenericClient for the given url.
     *
     * @param fhirVersionEnum the FHIR version to create a client for
     * @param url the server base url to connect to
     * @param serverValidationModeEnum the ServerValidationMode to use
     * @return IGenericClient for the given url, with the server validation mode set
     */
    public static IGenericClient forUrl(
            FhirVersionEnum fhirVersionEnum, String url, ServerValidationModeEnum serverValidationModeEnum) {
        checkNotNull(fhirVersionEnum, "fhirVersionEnum is required");
        checkNotNull(url, "url is required");
        checkNotNull(serverValidationModeEnum, "serverValidationModeEnum is required");

        return forUrl(FhirContext.forCached(fhirVersionEnum), url, serverValidationModeEnum);
    }

    /**
     * Creates an IGenericClient for the given url.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param url the server base url to connect to
     * @param serverValidationModeEnum the ServerValidationMode to use
     * @return IGenericClient for the given url, with the server validation mode set
     */
    public static IGenericClient forUrl(
            FhirContext fhirContext, String url, ServerValidationModeEnum serverValidationModeEnum) {
        checkNotNull(fhirContext);
        checkNotNull(url);
        checkNotNull(serverValidationModeEnum, "serverValidationModeEnum is required");

        fhirContext.getRestfulClientFactory().setServerValidationMode(serverValidationModeEnum);
        return fhirContext.newRestfulGenericClient(url);
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(org.hl7.fhir.dstu3.model.Endpoint endpoint) {
        checkNotNull(endpoint);

        return forEndpoint(FhirContext.forDstu3Cached(), endpoint);
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(FhirContext fhirContext, org.hl7.fhir.dstu3.model.Endpoint endpoint) {
        checkNotNull(fhirContext);
        checkNotNull(endpoint);

        IGenericClient client = forUrl(fhirContext, endpoint.getAddress());
        if (endpoint.hasHeader()) {
            List<String> headerList = endpoint.getHeader().stream()
                    .map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerHeaders(client, headerList);
        }
        return client;
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(org.hl7.fhir.r4.model.Endpoint endpoint) {
        checkNotNull(endpoint);

        return forEndpoint(FhirContext.forR4Cached(), endpoint);
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(FhirContext fhirContext, org.hl7.fhir.r4.model.Endpoint endpoint) {
        checkNotNull(fhirContext);
        checkNotNull(endpoint);

        IGenericClient client = forUrl(fhirContext, endpoint.getAddress());
        if (endpoint.hasHeader()) {
            List<String> headerList = endpoint.getHeader().stream()
                    .map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerHeaders(client, headerList);
        }
        return client;
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(org.hl7.fhir.r5.model.Endpoint endpoint) {
        checkNotNull(endpoint, "endpoint is required");

        return forEndpoint(FhirContext.forR4Cached(), endpoint);
    }

    /**
     * Creates an IGenericClient for the given Endpoint.
     *
     * @param fhirContext the FhirContext to use to create the client
     * @param endpoint the Endpoint to connect to
     * @return IGenericClient for the given Endpoint, with appropriate header interceptors set up
     */
    public static IGenericClient forEndpoint(FhirContext fhirContext, org.hl7.fhir.r5.model.Endpoint endpoint) {
        checkNotNull(fhirContext);
        checkNotNull(endpoint);

        IGenericClient client = forUrl(fhirContext, endpoint.getAddress());
        if (endpoint.hasHeader()) {
            List<String> headerList = endpoint.getHeader().stream()
                    .map(headerString -> headerString.asStringValue())
                    .collect(Collectors.toList());
            registerHeaders(client, headerList);
        }
        return client;
    }

    /**
     * Registers HeaderInjectionInterceptors on a client.
     *
     * @param client the client to add headers to
     * @param headers an Array of Strings representing headers to add
     */
    public static void registerHeaders(IGenericClient client, String... headers) {
        checkNotNull(client);

        registerHeaders(client, Arrays.asList(headers));
    }

    /**
     * Registers HeaderInjectionInterceptors on a client
     *
     * @param client the client to add headers to
     * @param headerList a List of Strings representing headers to add
     */
    public static void registerHeaders(IGenericClient client, List<String> headerList) {
        checkNotNull(client);
        checkNotNull(headerList);

        Map<String, String> headerMap = setupHeaderMap(headerList);
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            IClientInterceptor headInterceptor = new HeaderInjectionInterceptor(entry.getKey(), entry.getValue());
            client.registerInterceptor(headInterceptor);
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
    public static void registerBasicAuth(IGenericClient client, String username, String password) {
        checkNotNull(client, "client is required");

        if (username != null) {
            BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
            client.registerInterceptor(authInterceptor);
        }
    }

    /**
     * Registers BearerAuthInterceptors on a client. This is useful when you have a bearer token.
     *
     * @param client the client to register BearerToken authentication on
     * @param token the bearer token to register
     */
    public static void registerBearerTokenAuth(IGenericClient client, String token) {
        checkNotNull(client, "client is required");

        if (token != null) {
            BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(token);
            client.registerInterceptor(authInterceptor);
        }
    }

    public static void registerAdditionalRequestHeadersAuth(IGenericClient client, String username, String apiKey) {
        checkNotNull(client, "client is required");

        if (username != null && apiKey != null) {
            String authString = StringUtils.join(
                    "Basic ",
                    Base64.getEncoder()
                            .encodeToString(
                                    StringUtils.join(username, ":", apiKey).getBytes(StandardCharsets.UTF_8)));
            AdditionalRequestHeadersInterceptor authInterceptor = new AdditionalRequestHeadersInterceptor();
            authInterceptor.addHeaderValue("Authorization", authString);
        }
    }

    /**
     * Parses a list of headers into their constituent parts. Used to prep the headers for
     * registration with HeaderInjectionInterceptors
     *
     * @param headerList a List of Strings representing headers to create
     * @return key-value pairs of headers
     */
    static Map<String, String> setupHeaderMap(List<String> headerList) {
        checkNotNull(headerList, "headerList is required");

        Map<String, String> headerMap = new HashMap<String, String>();
        String leftAuth = null;
        String rightAuth = null;
        if (headerList.size() < 1 || headerList.isEmpty()) {
            leftAuth = null;
            rightAuth = null;
            headerMap.put(leftAuth, rightAuth);
        } else {
            for (String header : headerList) {
                if (!header.contains(":")) {
                    throw new IllegalArgumentException("Endpoint header must contain \":\" .");
                }
                String[] authSplit = header.split(":");
                leftAuth = authSplit[0];
                rightAuth = authSplit[1];
                headerMap.put(leftAuth, rightAuth);
            }
        }
        return headerMap;
    }
}
