package org.opencds.cqf.fhir.cr.hapi.r4.ra;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.BundleUtil;
import java.util.Objects;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.cr.hapi.r4.ISubmitDataProcessorFactory;

public class SubmitRemarkDataProvider {

    // Operation canonical name and RA MeasureReport profile canonical
    private static final String OPERATION_NAME = "$submit-remark-data";
    private static final String RA_MR_REMARK_BUNDLE_PROFILE =
            "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-remark-bundle";
    private static final String RA_MR_REMARK_PROFILE =
            "http://hl7.org/fhir/us/davinci-ra/StructureDefinition/ra-measurereport-with-remark";

    private final ISubmitDataProcessorFactory r4SubmitDataProcessorFactory;

    public SubmitRemarkDataProvider(ISubmitDataProcessorFactory r4SubmitDataProcessorFactory) {
        this.r4SubmitDataProcessorFactory =
                Objects.requireNonNull(r4SubmitDataProcessorFactory, "r4SubmitDataProcessorFactory is required");
    }

    /**
     * A Condition Category Remark may reference resources such as Practitioner and Condition, using
     * Patch to submit the remark may not be feasible. This operation is used to submit a Risk
     * Adjustment Coding Gap Report with one or more ccRemarks on at least one of the Condition
     * Categories, along with the relevant resources referenced by the ccRemark(s).
     * URL: [base]/Measure/$submit-remark-data
     * URL: [base]/Measure/[id]/$submit-remark-data
     *
     * @param requestDetails Autopopulated by HAPI.
     * @param id             The ID of the Measure to submit remark data
     * @param bundle         A Bundle that contains a Risk Adjustment Coding Gap Report with
     *                       ccRemark(s) on at least one of the Condition Categories and the
     *                       ccRemark referenced resources
     * @return A transaction response {@link Bundle}.
     */
    @Description(
            shortDefinition = "$submit-remark-data",
            value =
                    "Implements the <a href=\"https://build.fhir.org/ig/HL7/davinci-ra/OperationDefinition-submit-remark-data.html\">$submit-remark-data</a> operation.")
    @Operation(name = OPERATION_NAME, type = Measure.class)
    public Bundle submitRemarkData(
            RequestDetails requestDetails,
            @IdParam IdType id,
            @OperationParam(name = "bundle", min = 1) Bundle bundle) {

        // Validate required input
        validateResource(bundle, "bundle", "Bundle", RA_MR_REMARK_BUNDLE_PROFILE);

        // Extract MR from Bundle (should be the first entry in the Bundle) and validate
        var firstEntry = bundle.getEntryFirstRep().getResource();
        validateResource(firstEntry, null, "MeasureReport", RA_MR_REMARK_PROFILE);

        // Extract MeasureReport from Bundle and remark resources
        var report = (MeasureReport) firstEntry;
        var remarkResources = BundleUtil.toListOfResources(FhirContext.forR4Cached(), bundle).stream()
                .filter(resource -> !(resource instanceof MeasureReport))
                .toList();

        // TODO: Deduplicate remarkResources?

        // Delegate to implementation
        return r4SubmitDataProcessorFactory.create(requestDetails).submitData(report, remarkResources);
    }

    private static boolean hasProfile(Resource resource, String profile) {
        return resource.hasMeta()
                && resource.getMeta().hasProfile()
                && resource.getMeta().getProfile().stream()
                        .map(CanonicalType::asStringValue)
                        .anyMatch(profile::equals);
    }

    private static void validateResource(Resource resource, String parameterName, String resourceName, String profile) {
        if (resource == null) {
            throw new InvalidRequestException(
                    parameterName != null
                            ? String.format("Parameter '%s' must not be null.", parameterName)
                            : String.format("Resource '%s' must not be null.", resourceName));
        }
        validateProfile(resource, profile);
    }

    private static void validateProfile(Resource resource, String profile) {
        if (!hasProfile(resource, profile)) {
            String profiles = resource.hasMeta() && resource.getMeta().hasProfile()
                    ? resource.getMeta().getProfile().stream()
                            .map(CanonicalType::asStringValue)
                            .collect(Collectors.joining(", "))
                    : "(none)";
            throw new InvalidRequestException(String.format(
                    "The '%s' MUST conform to profile: %s. Provided profiles: %s.",
                    resource.fhirType(), profile, profiles));
        }
    }
}
