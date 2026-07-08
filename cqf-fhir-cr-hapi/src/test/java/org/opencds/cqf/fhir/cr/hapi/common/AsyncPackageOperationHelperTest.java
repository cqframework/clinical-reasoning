package org.opencds.cqf.fhir.cr.hapi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.IRestfulResponse;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.IRestfulServerDefaults;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

class AsyncPackageOperationHelperTest {

    private static final FhirContext CTX = FhirContext.forR4Cached();

    private static RequestDetails requestWithPrefer(String preferValue) {
        var requestDetails = mock(RequestDetails.class);
        when(requestDetails.getHeader(Constants.HEADER_PREFER)).thenReturn(preferValue);
        return requestDetails;
    }

    @Test
    void disabledHelper_neverAsync_andDoesNotReadRequest() {
        var helper = AsyncPackageOperationHelper.disabled();
        var requestDetails = mock(RequestDetails.class);

        assertFalse(helper.isAsyncRequested(requestDetails));
        // When async is disabled we short-circuit before touching the request at all.
        verifyNoInteractions(requestDetails);
    }

    @Test
    void isAsyncRequested_recognizesPreferRespondAsync() {
        var helper = new AsyncPackageOperationHelper(mock(PackageJobService.class));

        assertTrue(helper.isAsyncRequested(requestWithPrefer("respond-async")));
        assertTrue(helper.isAsyncRequested(requestWithPrefer("respond-async, handling=lenient")));
        assertTrue(helper.isAsyncRequested(requestWithPrefer("RESPOND-ASYNC")));
        assertFalse(helper.isAsyncRequested(requestWithPrefer("return=representation")));
        assertFalse(helper.isAsyncRequested(requestWithPrefer(null)));
    }

    @Test
    void buildPollUrl_appendsOperationAndJobId() {
        var requestDetails = mock(RequestDetails.class);
        when(requestDetails.getFhirServerBase()).thenReturn("http://localhost:8080/fhir");

        assertEquals(
                "http://localhost:8080/fhir/$package-status?_jobId=abc-123",
                AsyncPackageOperationHelper.buildPollUrl(requestDetails, "abc-123"));
    }

    @Test
    void buildPollUrl_doesNotDoubleSlashWhenBaseEndsWithSlash() {
        var requestDetails = mock(RequestDetails.class);
        when(requestDetails.getFhirServerBase()).thenReturn("http://localhost:8080/fhir/");

        assertEquals(
                "http://localhost:8080/fhir/$package-status?_jobId=abc-123",
                AsyncPackageOperationHelper.buildPollUrl(requestDetails, "abc-123"));
    }

    @Test
    void informationalOutcome_hasSingleInformationIssue() {
        var outcome = (OperationOutcome) AsyncPackageOperationHelper.informationalOutcome(CTX, "still working");

        assertNotNull(outcome);
        assertEquals(1, outcome.getIssue().size());
        assertEquals(
                OperationOutcome.IssueSeverity.INFORMATION,
                outcome.getIssueFirstRep().getSeverity());
    }

    @Test
    void synchronousRequest_writesBundleWith200_usingLiveRequest_andDoesNotSubmitJob() {
        var jobService = mock(PackageJobService.class);
        var helper = new AsyncPackageOperationHelper(jobService);

        var requestDetails = mock(RequestDetails.class);
        when(requestDetails.getHeader(Constants.HEADER_PREFER)).thenReturn(null);
        var server = mock(IRestfulServerDefaults.class);
        when(requestDetails.getServer()).thenReturn(server);
        when(requestDetails.isRespondGzip()).thenReturn(false);

        var bundle = new Bundle();
        var suppliedRequest = new AtomicReference<RequestDetails>();
        Function<RequestDetails, IBaseBundle> work = rd -> {
            suppliedRequest.set(rd);
            return bundle;
        };

        try (MockedStatic<RestfulServerUtils> util = mockStatic(RestfulServerUtils.class)) {
            helper.packageOrRespondAsync(requestDetails, work);

            util.verify(() -> RestfulServerUtils.streamResponseAsResource(
                    eq(server),
                    eq(bundle),
                    any(),
                    eq(Constants.STATUS_HTTP_200_OK),
                    eq(false),
                    eq(false),
                    eq(requestDetails)));
        }

        // The synchronous path runs the work against the live request and never queues a job.
        assertSame(requestDetails, suppliedRequest.get());
        verify(jobService, never()).submit(any());
    }

    @Test
    void asyncRequest_submitsJob_writes202_withContentLocation_andDoesNotRunWorkOnCallerThread() {
        var jobService = mock(PackageJobService.class);
        when(jobService.submit(any())).thenReturn("job-xyz");
        var helper = new AsyncPackageOperationHelper(jobService);

        var requestDetails = mock(RequestDetails.class);
        when(requestDetails.getHeader(Constants.HEADER_PREFER)).thenReturn(Constants.HEADER_PREFER_RESPOND_ASYNC);
        when(requestDetails.getFhirServerBase()).thenReturn("http://host/fhir");
        when(requestDetails.getFhirContext()).thenReturn(CTX);
        var response = mock(IRestfulResponse.class);
        when(requestDetails.getResponse()).thenReturn(response);
        when(requestDetails.getServer()).thenReturn(mock(IRestfulServerDefaults.class));

        var workRan = new AtomicBoolean(false);
        Function<RequestDetails, IBaseBundle> work = rd -> {
            workRan.set(true);
            return new Bundle();
        };

        try (MockedStatic<RestfulServerUtils> util = mockStatic(RestfulServerUtils.class)) {
            helper.packageOrRespondAsync(requestDetails, work);

            verify(jobService).submit(any());
            verify(response)
                    .addHeader(Constants.HEADER_CONTENT_LOCATION, "http://host/fhir/$package-status?_jobId=job-xyz");
            util.verify(() -> RestfulServerUtils.streamResponseAsResource(
                    any(IRestfulServerDefaults.class),
                    any(IBaseResource.class),
                    any(),
                    eq(Constants.STATUS_HTTP_202_ACCEPTED),
                    eq(false),
                    anyBoolean(),
                    eq(requestDetails)));
        }

        // submit() is stubbed, so the work must not have executed on the request thread.
        assertFalse(workRan.get());
    }

    @Test
    void asyncRequest_detachesToServletRequestDetails_preservingCallerContext() throws Exception {
        var jobService = new PackageJobService(1, Duration.ofMinutes(1));
        var helper = new AsyncPackageOperationHelper(jobService);

        var servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(Constants.HEADER_PREFER, Constants.HEADER_PREFER_RESPOND_ASYNC);
        servletRequest.addHeader("X-Tenant", "acme");

        var requestDetails = new ServletRequestDetails();
        requestDetails.setServletRequest(servletRequest);
        requestDetails.setServer(new RestfulServer(CTX));
        requestDetails.setFhirServerBase("http://host/fhir");
        requestDetails.setResponse(mock(IRestfulResponse.class));

        var captured = new AtomicReference<RequestDetails>();
        var done = new CountDownLatch(1);
        Function<RequestDetails, IBaseBundle> work = rd -> {
            captured.set(rd);
            done.countDown();
            return new Bundle();
        };

        try (MockedStatic<RestfulServerUtils> ignored = mockStatic(RestfulServerUtils.class)) {
            helper.packageOrRespondAsync(requestDetails, work);
            assertTrue(done.await(5, TimeUnit.SECONDS), "background work should run");
        } finally {
            jobService.shutdown();
        }

        var detached = captured.get();
        // Must stay a ServletRequestDetails so authorization/consent interceptors enforce as they do
        // synchronously — NOT a SystemRequestDetails, which they trust and skip.
        assertInstanceOf(ServletRequestDetails.class, detached);
        assertFalse(detached instanceof SystemRequestDetails, "async work must not run as a trusted system request");
        // Headers are snapshotted, so both accessors work off the recycled servlet request.
        var servletDetached = (ServletRequestDetails) detached;
        assertEquals(List.of("acme"), servletDetached.getHeaders("X-Tenant"));
        assertTrue(servletDetached.getHeaders().containsKey("X-Tenant"));
    }
}
