package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DetachedServletRequestDetailsTest {

    private static final FhirContext CTX = FhirContext.forR4Cached();

    private static ServletRequestDetails original() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Tenant", "acme");
        request.addHeader("Prefer", "respond-async");

        var details = new ServletRequestDetails();
        details.setServletRequest(request);
        details.setServer(new RestfulServer(CTX));
        details.setFhirServerBase("http://host/fhir");
        return details;
    }

    @Test
    void isServletRequestDetails_notSystemRequestDetails() {
        var detached = new DetachedServletRequestDetails(original());

        assertInstanceOf(ServletRequestDetails.class, detached);
        assertFalse(
                SystemRequestDetails.class.isInstance(detached),
                "must remain a client request so authorization/consent interceptors enforce");
    }

    @Test
    void swapsInDetachedServletRequest() {
        var detached = new DetachedServletRequestDetails(original());

        assertInstanceOf(DetachedHttpServletRequest.class, detached.getServletRequest());
    }

    @Test
    void headersReadableFromSnapshot() {
        var detached = new DetachedServletRequestDetails(original());

        assertEquals(List.of("acme"), detached.getHeaders("X-Tenant"));
        assertTrue(detached.getHeaders().containsKey("X-Tenant"));
    }

    @Test
    void survivesRepositoryStyleReCloning() {
        // The repository re-clones the request per DAO operation via the plain copy constructor. The
        // clone must still read headers from the snapshot rather than the (recycled) servlet request.
        var detached = new DetachedServletRequestDetails(original());

        var reClone = new ServletRequestDetails(detached);

        assertInstanceOf(DetachedHttpServletRequest.class, reClone.getServletRequest());
        assertTrue(reClone.getHeaders().containsKey("X-Tenant"));
        assertEquals(List.of("acme"), reClone.getHeaders("X-Tenant"));
    }
}
