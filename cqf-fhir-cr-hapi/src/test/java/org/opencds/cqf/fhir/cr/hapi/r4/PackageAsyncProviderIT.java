package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.fhir.cr.hapi.common.AsyncPackageOperationHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Verifies the FHIR Asynchronous Request Pattern for {@code $package}. Uses the raw HTTP client
 * (rather than the FHIR {@code IGenericClient}) so HTTP status codes and headers are observable.
 *
 * <p>Also guards the security invariant that the async path enforces the same authorization as the
 * synchronous path: the background work runs under a detached {@code ServletRequestDetails}, not a
 * trusted {@link SystemRequestDetails}, so a caller denied synchronously cannot obtain the package via
 * {@code Prefer: respond-async}.
 */
@ExtendWith(SpringExtension.class)
class PackageAsyncProviderIT extends BaseCrR4TestServer {

    private static final ContentType FHIR_JSON = ContentType.create("application/fhir+json", StandardCharsets.UTF_8);

    @Autowired
    IInterceptorService interceptorService;

    private final DenyNonSystemReads denyInterceptor = new DenyNonSystemReads();

    @AfterEach
    void removeDenyInterceptor() {
        interceptorService.unregisterInterceptor(denyInterceptor);
    }

    /**
     * Mirrors how HAPI's ConsentInterceptor / AuthorizationInterceptor behave: enforcement is skipped
     * for internal {@link SystemRequestDetails} calls and applied to real client requests.
     */
    @Interceptor
    static class DenyNonSystemReads {
        @Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
        public void preShow(RequestDetails theRequestDetails) {
            if (theRequestDetails instanceof SystemRequestDetails) {
                return;
            }
            throw new ForbiddenOperationException("Access denied by policy");
        }
    }

    @Test
    @SuppressWarnings("java:S2925")
    void asyncPackage_returns202ThenPollableBundle() throws Exception {
        loadBundle("small-naive-expansion-bundle.json");

        var post = new HttpPost(ourServerBase + "/Library/SmallSpecificationLibrary/$package");
        post.addHeader(Constants.HEADER_PREFER, Constants.HEADER_PREFER_RESPOND_ASYNC);
        post.setEntity(new StringEntity("{\"resourceType\":\"Parameters\"}", FHIR_JSON));

        String contentLocation;
        try (CloseableHttpResponse response = ourHttpClient.execute(post)) {
            assertEquals(
                    Constants.STATUS_HTTP_202_ACCEPTED,
                    response.getStatusLine().getStatusCode(),
                    "respond-async request should return 202 Accepted");
            var header = response.getFirstHeader(Constants.HEADER_CONTENT_LOCATION);
            assertNotNull(header, "202 response must carry a Content-Location header");
            contentLocation = header.getValue();
            assertTrue(
                    contentLocation.contains(AsyncPackageOperationHelper.PACKAGE_STATUS_OPERATION),
                    "Content-Location should point at the $package-status endpoint");
        }

        Bundle result = null;
        var statusCode = Constants.STATUS_HTTP_202_ACCEPTED;
        for (var attempt = 0; attempt < 80 && statusCode == Constants.STATUS_HTTP_202_ACCEPTED; attempt++) {
            var get = new HttpGet(contentLocation);
            get.addHeader(Constants.HEADER_ACCEPT, "application/fhir+json");
            try (CloseableHttpResponse pollResponse = ourHttpClient.execute(get)) {
                statusCode = pollResponse.getStatusLine().getStatusCode();
                if (statusCode == Constants.STATUS_HTTP_200_OK) {
                    var body = EntityUtils.toString(pollResponse.getEntity(), StandardCharsets.UTF_8);
                    result = ourParser.parseResource(Bundle.class, body);
                }
            }
            if (statusCode == Constants.STATUS_HTTP_202_ACCEPTED) {
                Thread.sleep(250);
            }
        }

        assertEquals(Constants.STATUS_HTTP_200_OK, statusCode, "Async $package job should complete with 200");
        assertNotNull(result, "Completed job should return a Bundle");
        assertTrue(result.hasEntry(), "Packaged Bundle should contain entries");
    }

    @Test
    void packageStatus_unknownJob_returns404() throws Exception {
        loadBundle("small-naive-expansion-bundle.json");

        var get = new HttpGet(ourServerBase + "/" + AsyncPackageOperationHelper.PACKAGE_STATUS_OPERATION + "?"
                + AsyncPackageOperationHelper.JOB_ID_PARAM + "=does-not-exist");
        try (CloseableHttpResponse response = ourHttpClient.execute(get)) {
            assertEquals(
                    Constants.STATUS_HTTP_404_NOT_FOUND,
                    response.getStatusLine().getStatusCode(),
                    "Unknown job id should return 404");
        }
    }

    @Test
    @SuppressWarnings("java:S2925")
    void asyncPackage_deniedSynchronously_isAlsoDeniedAsync() throws Exception {
        loadBundle("small-naive-expansion-bundle.json");
        // Register only after loading the fixture so the policy applies to the $package reads.
        interceptorService.registerInterceptor(denyInterceptor);

        var packageUrl = ourServerBase + "/Library/SmallSpecificationLibrary/$package";

        // 1) Synchronous $package is denied by the policy.
        var syncPost = new HttpPost(packageUrl);
        syncPost.setEntity(new StringEntity("{\"resourceType\":\"Parameters\"}", FHIR_JSON));
        int syncStatus;
        try (CloseableHttpResponse response = ourHttpClient.execute(syncPost)) {
            syncStatus = response.getStatusLine().getStatusCode();
        }
        assertEquals(
                Constants.STATUS_HTTP_403_FORBIDDEN,
                syncStatus,
                "synchronous $package should be denied by the authorization policy");

        // 2) Async $package must NOT succeed where the synchronous request was denied.
        var asyncPost = new HttpPost(packageUrl);
        asyncPost.addHeader(Constants.HEADER_PREFER, Constants.HEADER_PREFER_RESPOND_ASYNC);
        asyncPost.setEntity(new StringEntity("{\"resourceType\":\"Parameters\"}", FHIR_JSON));

        String contentLocation;
        try (CloseableHttpResponse response = ourHttpClient.execute(asyncPost)) {
            assertEquals(
                    Constants.STATUS_HTTP_202_ACCEPTED,
                    response.getStatusLine().getStatusCode(),
                    "async kick-off should be accepted");
            var header = response.getFirstHeader(Constants.HEADER_CONTENT_LOCATION);
            assertNotNull(header, "202 response must carry a Content-Location header");
            contentLocation = header.getValue();
        }

        var finalStatus = Constants.STATUS_HTTP_202_ACCEPTED;
        for (var attempt = 0; attempt < 80 && finalStatus == Constants.STATUS_HTTP_202_ACCEPTED; attempt++) {
            var get = new HttpGet(contentLocation);
            get.addHeader(Constants.HEADER_ACCEPT, "application/fhir+json");
            try (CloseableHttpResponse pollResponse = ourHttpClient.execute(get)) {
                finalStatus = pollResponse.getStatusLine().getStatusCode();
            }
            if (finalStatus == Constants.STATUS_HTTP_202_ACCEPTED) {
                Thread.sleep(250);
            }
        }

        // The async job runs under the caller's context, so the policy denies it too: the job fails
        // and the poll surfaces the denial rather than returning the packaged Bundle.
        assertNotEquals(
                Constants.STATUS_HTTP_200_OK,
                finalStatus,
                "async $package must not succeed where the synchronous request was denied");
    }
}
