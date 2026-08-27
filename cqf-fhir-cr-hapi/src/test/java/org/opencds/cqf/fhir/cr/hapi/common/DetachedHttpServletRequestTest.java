package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.DispatcherType;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class DetachedHttpServletRequestTest {

    private static MockHttpServletRequest source() {
        var source = new MockHttpServletRequest();
        source.setMethod("POST");
        source.setRequestURI("/fhir/Library/123/$package");
        source.setContextPath("/fhir");
        source.setServletPath("/Library");
        source.setPathInfo("/123/$package");
        source.setQueryString("_format=json");
        source.setScheme("http");
        source.setServerName("example.org");
        source.setServerPort(8080);
        source.setContentType("application/fhir+json");
        source.addHeader("X-Tenant", "acme");
        source.addHeader("Prefer", "respond-async");
        return source;
    }

    @Test
    void snapshotsHeaders_caseInsensitive_andIndependentOfSource() {
        var source = source();
        var detached = new DetachedHttpServletRequest(source);

        assertEquals("acme", detached.getHeader("X-Tenant"));
        assertEquals("acme", detached.getHeader("x-tenant"), "header lookup should be case-insensitive");
        assertEquals(List.of("acme"), Collections.list(detached.getHeaders("X-Tenant")));
        assertTrue(Collections.list(detached.getHeaderNames()).contains("X-Tenant"));
        assertFalse(detached.getHeaders("missing").hasMoreElements());
        assertNull(detached.getHeader("missing"));

        // Mutating the source after construction must not affect the snapshot.
        source.addHeader("X-Late", "nope");
        assertNull(detached.getHeader("X-Late"));
    }

    @Test
    void snapshotsRequestLineMetadata() {
        var detached = new DetachedHttpServletRequest(source());

        assertEquals("POST", detached.getMethod());
        assertEquals("/fhir/Library/123/$package", detached.getRequestURI());
        assertNotNull(detached.getRequestURL());
        assertEquals("/fhir", detached.getContextPath());
        assertEquals("/Library", detached.getServletPath());
        assertEquals("/123/$package", detached.getPathInfo());
        assertEquals("_format=json", detached.getQueryString());
        assertEquals("http", detached.getScheme());
        assertEquals("example.org", detached.getServerName());
        assertEquals(8080, detached.getServerPort());
        assertEquals("application/fhir+json", detached.getContentType());
    }

    @Test
    void intAndDateHeaders() {
        var source = source();
        source.addHeader("X-Count", "42");
        var detached = new DetachedHttpServletRequest(source);

        assertEquals(42, detached.getIntHeader("X-Count"));
        assertEquals(-1, detached.getIntHeader("missing"));
        assertEquals(-1, detached.getDateHeader("X-Tenant"));
    }

    @Test
    void attributes_areMutableAndIsolated() {
        var detached = new DetachedHttpServletRequest(source());

        assertNull(detached.getAttribute("k"));
        detached.setAttribute("k", "v");
        assertEquals("v", detached.getAttribute("k"));
        assertTrue(Collections.list(detached.getAttributeNames()).contains("k"));

        detached.setAttribute("k", null); // null value removes
        assertNull(detached.getAttribute("k"));

        detached.setAttribute("k2", "v2");
        detached.removeAttribute("k2");
        assertNull(detached.getAttribute("k2"));
    }

    @Test
    void body_isEmpty() throws Exception {
        var detached = new DetachedHttpServletRequest(source());

        var in = detached.getInputStream();
        assertTrue(in.isReady());
        assertTrue(in.isFinished());
        assertEquals(-1, in.read());
        assertDoesNotThrow(() -> in.setReadListener(null));
        assertNull(detached.getReader().readLine());
    }

    @Test
    void isSecure_reflectsScheme() {
        assertFalse(new DetachedHttpServletRequest(source()).isSecure());

        var secure = source();
        secure.setScheme("https");
        assertTrue(new DetachedHttpServletRequest(secure).isSecure());
    }

    @Test
    void asyncOperations_areUnsupported() {
        var detached = new DetachedHttpServletRequest(source());

        assertFalse(detached.isAsyncStarted());
        assertFalse(detached.isAsyncSupported());
        assertThrows(IllegalStateException.class, detached::startAsync);
        assertThrows(IllegalStateException.class, () -> detached.startAsync(null, null));
        assertThrows(IllegalStateException.class, detached::getAsyncContext);
    }

    @Test
    void httpAuthAndSessionOperations_areNoOps() {
        var detached = new DetachedHttpServletRequest(source());

        assertNull(detached.getAuthType());
        assertEquals(0, detached.getCookies().length);
        assertNull(detached.getRemoteUser());
        assertFalse(detached.isUserInRole("admin"));
        assertNull(detached.getUserPrincipal());
        assertNull(detached.getRequestedSessionId());
        assertNull(detached.getSession(false));
        assertNull(detached.getSession());
        assertNull(detached.changeSessionId());
        assertFalse(detached.isRequestedSessionIdValid());
        assertFalse(detached.isRequestedSessionIdFromCookie());
        assertFalse(detached.isRequestedSessionIdFromURL());
        assertFalse(assertDoesNotThrow(() -> detached.authenticate(null)));
        assertDoesNotThrow(() -> detached.login("u", "p"));
        assertDoesNotThrow(detached::logout);
        assertNull(assertDoesNotThrow(() -> detached.upgrade(null)));
        assertTrue(detached.getParts().isEmpty());
        assertNull(detached.getPart("file"));
    }

    @Test
    void inertServletDefaults() {
        var detached = new DetachedHttpServletRequest(source());

        assertNull(detached.getCharacterEncoding());
        assertDoesNotThrow(() -> detached.setCharacterEncoding("utf-8"));
        assertEquals(-1, detached.getContentLength());
        assertEquals(-1, detached.getContentLengthLong());
        assertNull(detached.getParameter("x"));
        assertFalse(detached.getParameterNames().hasMoreElements());
        assertArrayEquals(new String[0], detached.getParameterValues("x"));
        assertTrue(detached.getParameterMap().isEmpty());
        assertNull(detached.getRemoteAddr());
        assertNull(detached.getRemoteHost());
        assertEquals(0, detached.getRemotePort());
        assertNull(detached.getLocalName());
        assertNull(detached.getLocalAddr());
        assertEquals(0, detached.getLocalPort());
        assertNotNull(detached.getLocale());
        assertTrue(detached.getLocales().hasMoreElements());
        assertNull(detached.getRequestDispatcher("/x"));
        assertNull(detached.getServletContext());
        assertEquals(DispatcherType.REQUEST, detached.getDispatcherType());
        assertEquals("", detached.getRequestId());
        assertEquals("", detached.getProtocolRequestId());
        assertNull(detached.getServletConnection());
        assertNull(detached.getPathTranslated());
    }
}
