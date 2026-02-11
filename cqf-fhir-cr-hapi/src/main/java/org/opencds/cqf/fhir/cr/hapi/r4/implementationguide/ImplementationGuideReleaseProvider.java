package org.opencds.cqf.fhir.cr.hapi.r4.implementationguide;

import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RELEASE;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ImplementationGuideReleaseProvider {
    private final IImplementationGuideProcessorFactory implementationGuideProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public ImplementationGuideReleaseProvider(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        this.implementationGuideProcessorFactory = implementationGuideProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Sets the status of an existing artifact to Active if it has status Draft.
     *
     * @param id                 the {@link IdType IdType}, always an argument for instance level operations
     * @param version            new version in the form MAJOR.MINOR.PATCH
     * @param versionBehavior    how to handle differences between the user-provided and incumbent versions
     * @param latestFromTxServer whether to query the TxServer if version information is missing from references
     * @param requestDetails     the {@link RequestDetails RequestDetails}
     * @return A transaction bundle result of the updated resources
     */
    @Operation(name = CRMI_OPERATION_RELEASE, idempotent = true, global = true, type = ImplementationGuide.class)
    @Description(shortDefinition = CRMI_OPERATION_RELEASE, value = "Release an existing draft artifact")
    public IBaseBundle releaseImplementationGuide(
            @IdParam IdType id,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer") BooleanType latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "releaseLabel") StringType releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .releaseImplementationGuide(
                        Eithers.forMiddle3(id),
                        getReleaseParameters(
                                getStringValue(version),
                                versionBehavior,
                                latestFromTxServer,
                                requireNonExperimental,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                getStringValue(releaseLabel)));
    }

    @Operation(name = CRMI_OPERATION_RELEASE, idempotent = true, global = true, type = ImplementationGuide.class)
    @Description(shortDefinition = CRMI_OPERATION_RELEASE, value = "Release an existing draft artifact")
    public IBaseBundle releaseImplementationGuide(
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer") BooleanType latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "releaseLabel") StringType releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .releaseImplementationGuide(
                        Eithers.forMiddle3(getIdType(fhirVersion, "ImplementationGuide", id)),
                        getReleaseParameters(
                                getStringValue(version),
                                versionBehavior,
                                latestFromTxServer,
                                requireNonExperimental,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                getStringValue(releaseLabel)));
    }

    private static Parameters getReleaseParameters(
            String version,
            CodeType versionBehavior,
            BooleanType latestFromTxServer,
            CodeType requireNonExperimental,
            IBaseResource terminologyEndpoint,
            String releaseLabel) {
        var params = new Parameters();
        if (version != null) {
            params.addParameter("version", version);
        }
        if (versionBehavior != null) {
            params.addParameter("versionBehavior", versionBehavior);
        }
        if (latestFromTxServer != null && latestFromTxServer.hasValue()) {
            params.addParameter("latestFromTxServer", latestFromTxServer.getValue());
        }
        if (requireNonExperimental != null) {
            params.addParameter("requireNonExperimental", requireNonExperimental);
        }
        if (releaseLabel != null) {
            params.addParameter("releaseLabel", releaseLabel);
        }
        if (terminologyEndpoint != null) {
            params.addParameter().setName("terminologyEndpoint").setResource((Endpoint) terminologyEndpoint);
        }
        return params;
    }
}
