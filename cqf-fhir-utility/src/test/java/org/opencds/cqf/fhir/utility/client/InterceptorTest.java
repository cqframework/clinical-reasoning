package org.opencds.cqf.fhir.utility.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterceptorTest {

    @Test
    void headerInjectionSingleHeader() {
        var interceptor = new HeaderInjectionInterceptor("Authorization", "Bearer token");
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        verify(request).addHeader("Authorization", "Bearer token");
    }

    @Test
    void headerInjectionMapHeaders() {
        var headers = Map.of("X-Custom", "val1", "Accept", "application/json");
        var interceptor = new HeaderInjectionInterceptor(headers);
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        verify(request).addHeader("X-Custom", "val1");
        verify(request).addHeader("Accept", "application/json");
    }

    @Test
    void headerInjectionResponseNoOp() throws Exception {
        var interceptor = new HeaderInjectionInterceptor("key", "val");
        var response = mock(IHttpResponse.class);
        interceptor.interceptResponse(response);
        // no exception = pass
    }

    @Test
    void additionalRequestHeadersDefault() {
        var interceptor = new AdditionalRequestHeadersInterceptor();
        interceptor.addHeaderValue("X-Test", "value1");
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        verify(request).addHeader("X-Test", "value1");
    }

    @Test
    void additionalRequestHeadersFromMap() {
        var headers = Map.of("X-Foo", List.of("bar", "baz"));
        var interceptor = new AdditionalRequestHeadersInterceptor(headers);
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        verify(request).addHeader("X-Foo", "bar");
        verify(request).addHeader("X-Foo", "baz");
    }

    @Test
    void additionalRequestHeadersNullMap() {
        var interceptor = new AdditionalRequestHeadersInterceptor(null);
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        // no headers added, no exception
    }

    @Test
    void additionalRequestHeadersAddAll() {
        var interceptor = new AdditionalRequestHeadersInterceptor();
        interceptor.addAllHeaderValues("X-Multi", List.of("a", "b"));
        var request = mock(IHttpRequest.class);
        interceptor.interceptRequest(request);
        verify(request).addHeader("X-Multi", "a");
        verify(request).addHeader("X-Multi", "b");
    }
}
