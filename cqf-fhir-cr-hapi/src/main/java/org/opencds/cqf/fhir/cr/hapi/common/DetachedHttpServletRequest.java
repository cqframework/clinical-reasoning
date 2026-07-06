package org.opencds.cqf.fhir.cr.hapi.common;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A self-contained, read-only snapshot of an {@link HttpServletRequest}, safe to use on a worker
 * thread after the servlet container has recycled the original request.
 *
 * <p>Only the header, attribute, and request-line state that the repository/interceptor layer reads
 * during a {@code $package} build is captured; everything else returns an inert default. It exists
 * so that {@link DetachedServletRequestDetails} — and any per-operation copy the repository makes of
 * it — can read headers without dereferencing the recycled Jetty request.
 */
class DetachedHttpServletRequest implements HttpServletRequest {

    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final String method;
    private final String requestUri;
    private final StringBuilder requestUrl;
    private final String contextPath;
    private final String servletPath;
    private final String pathInfo;
    private final String queryString;
    private final String scheme;
    private final String serverName;
    private final int serverPort;
    private final String protocol;
    private final String contentType;

    DetachedHttpServletRequest(HttpServletRequest source) {
        var names = source.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                var name = names.nextElement();
                headers.put(name, Collections.list(source.getHeaders(name)));
            }
        }
        method = source.getMethod();
        requestUri = source.getRequestURI();
        var url = source.getRequestURL();
        requestUrl = url == null ? null : new StringBuilder(url);
        contextPath = source.getContextPath();
        servletPath = source.getServletPath();
        pathInfo = source.getPathInfo();
        queryString = source.getQueryString();
        scheme = source.getScheme();
        serverName = source.getServerName();
        serverPort = source.getServerPort();
        protocol = source.getProtocol();
        contentType = source.getContentType();
    }

    // --- Headers -----------------------------------------------------------------------------

    @Override
    public String getHeader(String name) {
        var values = getMatching(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        var values = getMatching(name);
        return Collections.enumeration(values == null ? List.of() : values);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        var value = getHeader(name);
        return value == null ? -1 : Integer.parseInt(value);
    }

    @Override
    public long getDateHeader(String name) {
        return -1;
    }

    private List<String> getMatching(String name) {
        for (var entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return Collections.emptyList();
    }

    // --- Request line / metadata -------------------------------------------------------------

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getRequestURI() {
        return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer(requestUrl == null ? "" : requestUrl.toString());
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    // --- Attributes --------------------------------------------------------------------------

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    // --- Body (empty) ------------------------------------------------------------------------

    @Override
    public ServletInputStream getInputStream() {
        var delegate = new ByteArrayInputStream(new byte[0]);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op: the snapshot carries no body
            }

            @Override
            public int read() {
                return delegate.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8));
    }

    // --- Inert defaults ----------------------------------------------------------------------

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String env) {
        // no-op
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        return -1;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.emptyEnumeration();
    }

    @Override
    public String[] getParameterValues(String name) {
        return new String[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Map.of();
    }

    @Override
    public String getRemoteAddr() {
        return null;
    }

    @Override
    public String getRemoteHost() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getLocalName() {
        return null;
    }

    @Override
    public String getLocalAddr() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(List.of(Locale.getDefault()));
    }

    @Override
    public boolean isSecure() {
        return "https".equalsIgnoreCase(scheme);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() {
        throw new IllegalStateException("Async not supported on a detached request");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        throw new IllegalStateException("Async not supported on a detached request");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("Async not supported on a detached request");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getRequestId() {
        return "";
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    // --- HTTP-specific inert defaults --------------------------------------------------------

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public java.security.Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        return false;
    }

    @Override
    public void login(String username, String password) {
        // no-op
    }

    @Override
    public void logout() {
        // no-op
    }

    @Override
    public java.util.Collection<Part> getParts() {
        return List.of();
    }

    @Override
    public Part getPart(String name) {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        return null;
    }
}
