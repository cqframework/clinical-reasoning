package org.opencds.cqf.cql.evaluator.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This class creates a FHIR Rest API IGenericClient for a given fhirContext an
 * url.
 */
@Named
public class ClientFactory {

    protected FhirContext fhirContext;

    @Inject
	public ClientFactory(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }
    
    public IGenericClient create(String url, List<String> headers) {
        IGenericClient client = this.fhirContext.newRestfulGenericClient(url);
        registerAuth(client, headers);
        return client;
    }

    private void registerAuth(IGenericClient client, List<String> headers) {
        if (headers == null) {
            return;
        }

        Map<String, String> headerMap = setupHeaderMap(headers);
        AdditionalRequestHeadersInterceptor interceptor = new AdditionalRequestHeadersInterceptor();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            interceptor.addHeaderValue(entry.getKey(), entry.getValue());
        }
        client.registerInterceptor(interceptor);
    }

    private Map<String, String> setupHeaderMap(List<String> headers) {
        Map<String, String> headerMap = new HashMap<String, String>();
        String leftAuth = null;
        String rightAuth = null;
        if (headers.size() < 1 || headers.isEmpty()) {
            leftAuth = null;
            rightAuth = null;
            headerMap.put(leftAuth, rightAuth);
        } else {
            for (String header : headers) {
                if (!header.contains(":")) {
                    throw new RuntimeException("Endpoint header must contain \":\" .");
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
