package org.opencds.cqf.fhir.cr.hapi.common;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

/**
 * System-level poll endpoint for asynchronous {@code $package} jobs, implementing the status side
 * of the <a href="https://hl7.org/fhir/async.html">FHIR Asynchronous Request Pattern</a>.
 *
 * <p>Invoked as {@code GET [base]/$package-status?_jobId={id}}. While the job is running it returns
 * {@code 202 Accepted} (echoing the {@code Content-Location}); on success it returns {@code 200} with
 * the packaged Bundle; on failure it returns {@code 500} with an OperationOutcome.
 *
 * <p>The method is {@code manualResponse = true} so it owns the HTTP status code (200 vs 202 vs 500).
 */
public class PackageJobStatusProvider {

    private static final int STATUS_HTTP_500_INTERNAL_ERROR = 500;

    private final PackageJobService jobService;

    public PackageJobStatusProvider(PackageJobService jobService) {
        this.jobService = jobService;
    }

    @Operation(
            name = AsyncPackageOperationHelper.PACKAGE_STATUS_OPERATION,
            idempotent = true,
            manualResponse = true,
            global = true)
    public void packageStatus(
            @OperationParam(name = AsyncPackageOperationHelper.JOB_ID_PARAM, typeName = "string")
                    IPrimitiveType<String> jobId,
            RequestDetails requestDetails) {

        if (jobId == null
                || jobId.getValueAsString() == null
                || jobId.getValueAsString().isBlank()) {
            throw new InvalidRequestException(
                    "The '%s' parameter is required.".formatted(AsyncPackageOperationHelper.JOB_ID_PARAM));
        }

        var job = jobService.get(jobId.getValueAsString());
        if (job == null) {
            throw new ResourceNotFoundException("No $package job found with id: " + jobId.getValueAsString());
        }

        // Read a single consistent snapshot so status/result/error can't shift between checks.
        var snapshot = job.snapshot();
        switch (snapshot.status()) {
            case COMPLETED ->
                AsyncPackageOperationHelper.writeResource(
                        requestDetails, snapshot.result(), Constants.STATUS_HTTP_200_OK);
            case FAILED -> writeFailure(requestDetails, snapshot.error());
            default -> writeInProgress(requestDetails, job.getId());
        }
    }

    private void writeInProgress(RequestDetails requestDetails, String jobId) {
        requestDetails
                .getResponse()
                .addHeader(
                        Constants.HEADER_CONTENT_LOCATION,
                        AsyncPackageOperationHelper.buildPollUrl(requestDetails, jobId));
        var outcome = AsyncPackageOperationHelper.informationalOutcome(
                requestDetails.getFhirContext(), "The $package job is still processing.");
        AsyncPackageOperationHelper.writeResource(requestDetails, outcome, Constants.STATUS_HTTP_202_ACCEPTED);
    }

    private void writeFailure(RequestDetails requestDetails, String error) {
        var ctx = requestDetails.getFhirContext();
        var outcome = OperationOutcomeUtil.newInstance(ctx);
        OperationOutcomeUtil.addIssue(
                ctx, outcome, "error", error == null ? "The $package job failed." : error, null, "exception");
        AsyncPackageOperationHelper.writeResource(requestDetails, outcome, STATUS_HTTP_500_INTERNAL_ERROR);
    }
}
