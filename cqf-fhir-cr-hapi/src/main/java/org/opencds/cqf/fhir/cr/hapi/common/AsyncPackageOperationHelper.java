package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import java.io.IOException;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Bridges the synchronous {@code $package} provider methods to the
 * <a href="https://hl7.org/fhir/async.html">FHIR Asynchronous Request Pattern</a>.
 *
 * <p>Provider methods that delegate here must be declared {@code @Operation(manualResponse = true)}
 * and return {@code void}: in that mode HAPI performs no serialization and sets no status code, so
 * this helper owns the entire HTTP response. The synchronous branch is written through HAPI's own
 * {@link RestfulServerUtils#streamResponseAsResource} which preserves the non-async behaviour.
 *
 * <p>When a request carries {@code Prefer: respond-async} (and async is enabled), the package work
 * is detached from the live request and handed to {@link PackageJobService}; the client receives an
 * immediate {@code 202 Accepted} with a {@code Content-Location} header pointing at the
 * {@code $package-status} poll endpoint.
 */
public class AsyncPackageOperationHelper {

    public static final String PACKAGE_STATUS_OPERATION = "$package-status";
    public static final String JOB_ID_PARAM = "_jobId";

    /** When {@code null}, async is disabled and every request is served synchronously. */
    private final PackageJobService jobService;

    public AsyncPackageOperationHelper(PackageJobService jobService) {
        this.jobService = jobService;
    }

    /** A helper that always serves synchronously — used where async has not been wired in. */
    public static AsyncPackageOperationHelper disabled() {
        return new AsyncPackageOperationHelper(null);
    }

    public boolean isAsyncRequested(RequestDetails requestDetails) {
        if (jobService == null) {
            return false;
        }
        var prefer = requestDetails.getHeader(Constants.HEADER_PREFER);
        return prefer != null && prefer.toLowerCase().contains(Constants.HEADER_PREFER_RESPOND_ASYNC);
    }

    /**
     * Execute (or asynchronously schedule) a package operation and write the response.
     *
     * @param requestDetails the live request
     * @param work performs the package operation against the supplied {@link RequestDetails}. For a
     *     synchronous request this is the live request; for an async request it is a detached copy
     *     safe to use after the originating request has returned.
     */
    public void packageOrRespondAsync(RequestDetails requestDetails, Function<RequestDetails, IBaseBundle> work) {
        if (isAsyncRequested(requestDetails)) {
            var jobId = jobService.submit(() -> work.apply(detach(requestDetails)));
            writeAccepted(requestDetails, jobId);
        } else {
            writeResource(requestDetails, work.apply(requestDetails), Constants.STATUS_HTTP_200_OK);
        }
    }

    private void writeAccepted(RequestDetails requestDetails, String jobId) {
        requestDetails.getResponse().addHeader(Constants.HEADER_CONTENT_LOCATION, buildPollUrl(requestDetails, jobId));
        var outcome = informationalOutcome(
                requestDetails.getFhirContext(),
                "Request accepted for asynchronous processing. Poll the Content-Location URL for the result.");
        writeResource(requestDetails, outcome, Constants.STATUS_HTTP_202_ACCEPTED);
    }

    /** Build the absolute poll URL for the {@code $package-status} system operation. */
    static String buildPollUrl(RequestDetails requestDetails, String jobId) {
        var base = requestDetails.getFhirServerBase();
        var separator = base != null && base.endsWith("/") ? "" : "/";
        return base + separator + PACKAGE_STATUS_OPERATION + "?" + JOB_ID_PARAM + "=" + jobId;
    }

    static IBaseOperationOutcome informationalOutcome(FhirContext ctx, String message) {
        var outcome = OperationOutcomeUtil.newInstance(ctx);
        OperationOutcomeUtil.addIssue(ctx, outcome, "information", message, null, "informational");
        return outcome;
    }

    /**
     * Stream a resource as the HTTP response using HAPI's standard serialization path, faithfully
     * reproducing what HAPI would otherwise have done for a returned resource (summary mode,
     * content negotiation, gzip), but with an explicit status code.
     */
    static void writeResource(RequestDetails requestDetails, IBaseResource resource, int statusCode) {
        try {
            RestfulServerUtils.streamResponseAsResource(
                    requestDetails.getServer(),
                    resource,
                    RestfulServerUtils.determineSummaryMode(requestDetails),
                    statusCode,
                    false,
                    requestDetails.isRespondGzip(),
                    requestDetails);
        } catch (IOException e) {
            throw new InternalErrorException("Failed to write $package response: " + e.getMessage(), e);
        }
    }

    /**
     * Produce a {@link RequestDetails} that is safe to use on a worker thread after the originating
     * request has completed.
     */
    private static RequestDetails detach(RequestDetails requestDetails) {
        var detached = new SystemRequestDetails(requestDetails);
        if (detached.getFhirContext() == null) {
            detached.setFhirContext(requestDetails.getFhirContext());
        }
        if (requestDetails instanceof ServletRequestDetails servletRequestDetails) {
            var headers = servletRequestDetails.getHeaders();
            if (headers != null) {
                headers.forEach(detached::setHeaders);
            }
        }
        return detached;
    }
}
