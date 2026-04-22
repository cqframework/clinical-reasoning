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
     * <p>
     * This operation is designed to be used as part of the following workflow:
     * <ol>
     *   <li>{@code ImplementationGuide/$data-requirements} — analyzes an IG and produces a
     *       module-definition Library with all dependencies, including key element classification
     *       and ValueSet compose chain walking</li>
     *   <li>{@code Library/$infer-manifest-parameters} — converts the module-definition Library
     *       into an asset-collection manifest with expansion parameters and depends-on entries</li>
     *   <li>{@code Library/$release-manifest} — releases the manifest by resolving unversioned
     *       dependencies via the terminology endpoint and updating metadata (version, status, date)</li>
     * </ol>
     * <p>
     * The {@code terminologyEndpoint} parameter should provide an {@link org.hl7.fhir.r4.model.Endpoint}
     * with authentication headers for resolving terminology resources (e.g., VSAC). Set
     * {@code latestFromTxServer=true} to enable terminology server resolution of unversioned
     * ValueSet and CodeSystem references.
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
