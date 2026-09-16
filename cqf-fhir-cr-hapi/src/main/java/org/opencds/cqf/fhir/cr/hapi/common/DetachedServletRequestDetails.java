package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

/**
 * A {@link ServletRequestDetails} that is safe to use on a worker thread after the originating HTTP
 * request has been recycled by the servlet container.
 *
 * <p>The async {@code $package} path must run its repository access under the <em>same</em> request
 * identity as the synchronous path, so authorization and consent interceptors enforce identically.
 * Those interceptors treat a {@link ca.uhn.fhir.rest.api.server.SystemRequestDetails} as a trusted
 * internal call and skip enforcement, so the detached request must remain a {@code ServletRequestDetails}
 * — otherwise a caller denied synchronously could obtain the package via {@code Prefer: respond-async}.
 *
 * <p>The obstacle to reusing a {@code ServletRequestDetails} off-thread is that its headers are read
 * from the underlying servlet request, which is null once recycled — and the repository re-clones the
 * request per operation via the plain copy constructor, so overriding accessors here would not
 * survive. Instead this swaps in a {@link DetachedHttpServletRequest} snapshot (taken on the request
 * thread): both this instance and any copy the repository makes read headers from the snapshot rather
 * than the recycled request.
 */
class DetachedServletRequestDetails extends ServletRequestDetails {

    DetachedServletRequestDetails(ServletRequestDetails original) {
        // Invoked on the request thread, while the servlet request is still valid: the copy
        // constructor carries over user data, tenant, partition, server base, operation metadata and
        // the request body.
        super(original);
        // Replace the soon-to-be-recycled servlet request with a self-contained snapshot so header
        // reads (including those on the repository's per-operation copies of this request) stay valid.
        setServletRequest(new DetachedHttpServletRequest(original.getServletRequest()));
    }
}
