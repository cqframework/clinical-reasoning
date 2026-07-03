package org.opencds.cqf.fhir.cr.hapi.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.api.Constants;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opencds.cqf.fhir.cr.hapi.common.AsyncPackageOperationHelper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Verifies the FHIR Asynchronous Request Pattern for {@code $package}. Uses the raw HTTP client
 * (rather than the FHIR {@code IGenericClient}) so HTTP status codes and headers are observable.
 */
@ExtendWith(SpringExtension.class)
class PackageAsyncProviderTest extends BaseCrR4TestServer {

    private static final ContentType FHIR_JSON = ContentType.create("application/fhir+json", StandardCharsets.UTF_8);

    @Test
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
}
