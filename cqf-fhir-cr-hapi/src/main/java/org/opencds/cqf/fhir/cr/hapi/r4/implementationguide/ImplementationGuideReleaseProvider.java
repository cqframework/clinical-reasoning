package org.opencds.cqf.fhir.cr.hapi.r4.implementationguide;

import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Parameters;
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
    @Operation(name = "$release", idempotent = true, global = true, type = ImplementationGuide.class)
    @Description(shortDefinition = "$release", value = "Release an existing draft artifact")
    public IBaseBundle releaseImplementationGuide(
            @IdParam IdType id,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer") BooleanType latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            @OperationParam(name = "releaseLabel") String releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        var params = getReleaseParameters(
                getStringValue(version),
                versionBehavior,
                latestFromTxServer,
                requireNonExperimental,
                terminologyEndpoint,
                releaseLabel);
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .releaseImplementationGuide(Eithers.for3(null, id, null), params);
    }

    @Operation(name = "$release", idempotent = true, global = true, type = ImplementationGuide.class)
    @Description(shortDefinition = "$release", value = "Release an existing draft artifact")
    public IBaseBundle releaseImplementationGuide(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer") BooleanType latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            @OperationParam(name = "releaseLabel") String releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        var idToUse = (IdType) getIdType(fhirVersion, "ImplementationGuide", id);
        var params = getReleaseParameters(
                version,
                versionBehavior,
                latestFromTxServer,
                requireNonExperimental,
                terminologyEndpoint,
                releaseLabel);
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .releaseImplementationGuide(Eithers.for3(null, idToUse, null), params);
    }

    private static Parameters getReleaseParameters(
            String version,
            CodeType versionBehavior,
            BooleanType latestFromTxServer,
            CodeType requireNonExperimental,
            Endpoint terminologyEndpoint,
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
            params.addParameter().setName("terminologyEndpoint").setResource(terminologyEndpoint);
        }
        return params;
    }
}
