package org.opencds.cqf.fhir.cr.hapi.r4.ra;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.opencds.cqf.fhir.cr.hapi.r4.ISubmitDataProcessorFactory;

public class SubmitDataProvider {

    // Operation canonical name and RA MeasureReport profile canonical
    private static final String OPERATION_NAME = "$ra-submit-data";
    private static final String RA_DATAEX_MR_PROFILE =
            "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-datax-measurereport";

    private final ISubmitDataProcessorFactory r4SubmitDataProcessorFactory;

    public SubmitDataProvider(ISubmitDataProcessorFactory r4SubmitDataProcessorFactory) {
        this.r4SubmitDataProcessorFactory =
                Objects.requireNonNull(r4SubmitDataProcessorFactory, "r4SubmitDataProcessorFactory is required");
    }

    /**
     * The $ra-submit-data operation is used to submit a Risk Adjustment Data Exchange
     * MeasureReport and related data-of-interest resources.
     * Endpoint: [base]/Measure/$ra-submit-data
     * Deviation from base $submit-data:
     * - The {@code measureReport} input MUST conform to the
     *   <a href="http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-datax-measurereport">RA Data Exchange MeasureReport profile</a>
     *
     * @param requestDetails Autopopulated by HAPI.
     * @param report         The RA Data Exchange MeasureReport (1..1).
     * @param resources      Related data-of-interest resources (0..*).
     * @return A transaction response {@link Bundle}.
     */
    @Description(
            shortDefinition = "$ra-submit-data",
            value =
                    "Implements the <a href=\"http://hl7.org/fhir/us/davinci-ra/OperationDefinition/submit-data\">$ra-submit-data</a> operation.")
    @Operation(name = OPERATION_NAME, type = Measure.class)
    public Bundle submitData(
            RequestDetails requestDetails,
            @OperationParam(name = "measureReport", min = 1, max = 1) MeasureReport report,
            @OperationParam(name = "resource") List<IBaseResource> resources) {

        // Validate required input
        if (report == null) {
            throw new InvalidRequestException("Parameter 'measureReport' must not be null.");
        }
        if (!hasProfile(report)) {
            String profiles = report.hasMeta() && report.getMeta().hasProfile()
                    ? report.getMeta().getProfile().stream()
                            .map(CanonicalType::asStringValue)
                            .collect(Collectors.joining(", "))
                    : "(none)";
            throw new InvalidRequestException("The 'measureReport' MUST conform to profile: " + RA_DATAEX_MR_PROFILE
                    + ". Provided profiles: " + profiles + ".");
        }

        // Normalize optional inputs
        List<IBaseResource> safeResources = resources == null
                ? Collections.emptyList()
                : resources.stream().filter(Objects::nonNull).collect(Collectors.toList());

        // Delegate to implementation
        return r4SubmitDataProcessorFactory.create(requestDetails).submitData(report, safeResources);
    }

    private static boolean hasProfile(MeasureReport report) {
        return report.hasMeta()
                && report.getMeta().hasProfile()
                && report.getMeta().getProfile().stream()
                        .map(CanonicalType::asStringValue)
                        .anyMatch(SubmitDataProvider.RA_DATAEX_MR_PROFILE::equals);
    }
}
