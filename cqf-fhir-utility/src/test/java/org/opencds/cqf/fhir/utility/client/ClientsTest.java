package org.opencds.cqf.fhir.utility.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;

class ClientsTest {
    @Test
    void createClient() {
        IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");

        assertNotNull(client);
        assertEquals("http://test.com", client.getServerBase());
    }

    @Test
    void registerAuth() {
        IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
        Clients.registerBasicAuth(client, "user", "password");

        List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

        Object authInterceptor = interceptors.get(0);
        assertTrue(authInterceptor instanceof BasicAuthInterceptor);
    }

    @Test
    void registerHeaders() {
        IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
        Clients.registerHeaders(client, "Basic: XYZ123");

        List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

        Object interceptor = interceptors.get(0);
        assertTrue(interceptor instanceof HeaderInjectionInterceptor);
    }

    @Test
    void rejectInvalidHeaders() {
        IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
        assertThrows(IllegalArgumentException.class, () -> {
            Clients.registerHeaders(client, "BasicXYZ123");
        });
    }

    @Test
    void clientForEndpoint() {
        Endpoint endpoint = new Endpoint();
        endpoint.setAddress("http://test.com");

        endpoint.setHeader(Collections.singletonList(new StringType("Basic: XYZ123")));
        IGenericClient client = Clients.forEndpoint(endpoint);

        assertEquals("http://test.com", client.getServerBase());
        List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

        Object interceptor = interceptors.get(0);
        assertTrue(interceptor instanceof HeaderInjectionInterceptor);
    }

    @Test
    void registerAdditionalRequestHeadersAuth() {
        IGenericClient client = Clients.forUrl(FhirContext.forR4Cached(), "http://test.com");
        String userName = "someUser";
        String apiKey = "some-api-key";

        Clients.registerAdditionalRequestHeadersAuth(client, userName, apiKey);

        List<Object> interceptors = client.getInterceptorService().getAllRegisteredInterceptors();

        Object interceptor = interceptors.get(0);
        assertTrue(interceptor instanceof AdditionalRequestHeadersInterceptor);
    }
}
