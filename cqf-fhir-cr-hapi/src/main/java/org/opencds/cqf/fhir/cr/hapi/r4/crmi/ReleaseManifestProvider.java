package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_RELEASE_MANIFEST;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.r4.IReleaseManifestServiceFactory;

public class ReleaseManifestProvider {

    private final IReleaseManifestServiceFactory serviceFactory;
    private final FhirVersionEnum fhirVersion;

    public ReleaseManifestProvider(IReleaseManifestServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * The $release-manifest operation releases a manifest Library (asset-collection) that has
     * pre-computed depends-on entries. Unlike $release, this operation does not re-discover
     * dependencies through component traversal. Instead, it resolves unversioned dependency
     * references using the terminology endpoint and updates manifest metadata for release.
     */
    @Operation(name = CRMI_OPERATION_RELEASE_MANIFEST, idempotent = true, type = Library.class)
    @Description(
            shortDefinition = CRMI_OPERATION_RELEASE_MANIFEST,
            value = "Release a manifest Library with pre-computed dependencies")
    public Bundle releaseManifestOperation(
            @IdParam IdType id,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer", typeName = "Boolean")
                    IPrimitiveType<Boolean> latestFromTxServer,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "releaseLabel") StringType releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        return serviceFactory
                .create(requestDetails)
                .releaseManifest(
                        id,
                        getStringValue(version),
                        versionBehavior,
                        latestFromTxServer,
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint),
                        getStringValue(releaseLabel));
    }
}
