package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.r4.IReleaseServiceFactory;

public class ReleaseProvider {

    private final IReleaseServiceFactory r4ReleaseServiceFactory;
    private final FhirVersionEnum fhirVersion;

    public ReleaseProvider(IReleaseServiceFactory r4ReleaseServiceFactory) {
        this.r4ReleaseServiceFactory = r4ReleaseServiceFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    // TODO: This operation appears to be missing parameters defined in the CRMI specification:
    // CRMI 1.0.0: requireVersionSpecificReferences, requireActiveReferences, experimentalBehavior
    // CRMI 2.0.0: requireVersionSpecificReferences, requireActiveReferences, experimentalBehavior, releaseDate
    // TODO: requireNonExperimental may be synonymous with experimentalBehavior, but we should align with the spec
    // TODO: We should make use of the authoritativeSource extension over terminologyEndpoint if possible
    /**
     * The release operation supports updating the status of an existing draft artifact to active.
     * The operation sets the date element of the resource and pins versions of all direct and
     * transitive references and records them in the program’s manifest. Child artifacts (i.e.
     * artifacts of which the existing artifact is composed) are also released, recursively.
     * The release operation supports the ability of an authoring repository to transition an
     * artifact and, transitively, any referenced and owned (as indicated by the 'crmiOwned'
     * extension on the RelatedArtifact reference) component artifacts to a released state. The
     * operation SHALL update the status of all owned components to 'active' and update their date
     * to the current date. The operation SHALL ensure that all references for which a version is
     * determined are recorded in the version manifest. For both components and dependencies, if
     * versions are not specified in the relevant reference, the operation will look up the version
     * to be used in the version manifest.
     * When 'requireVersionSpecificReferences' is true then all references SHALL either be
     * version-specific or, if they are not, an entry SHALL exist in the version manifest to specify
     * which version of the referenced resource should be used. If,
     * 'requireVersionSpecificReferences' is true and there exists a reference that is not
     * version-specific and no entry exists in the version manifest for the referenced resource, the
     * program is considered to be in an invalid state and not eligible for release. If
     * 'requireVersionSpecificReferences' is false (the default), then unversioned references are
     * valid and the artifact can be released in that state - deferring the version determination to
     * the consumer.
     * When 'requireActiveReferences' is true then the operation SHALL throw an error if any 'draft'
     * or 'retired' dependencies are found.
     * @param id                    The logical id of the artifact to release. The server must know
     *                              the artifact (e.g. it is defined explicitly in the server's
     *                              artifacts)
     * @param version               Specifies the version to be applied—based on the version
     *                              behavior specified—to the artifact being released and any
     *                              referenced owned components.
     * @param versionBehavior       Indicates the behavior with which the 'version' parameter
     *                              should apply to the artifact being released and its components.
     * @param latestFromTxServer    Indicates whether the terminology server from which a value set
     *                              was originally downloaded should be checked for the latest
     *                              version. The terminology server of origin is tracked via the
     *                              <a href="https://hl7.org/fhir/extension-valueset-authoritativesource.html">authoritativeSource</a>
     *                              extension on the value set. If this flag is set to false or the
     *                              value set does not have an authoritativeSource specified, then
     *                              the check should be constrained to the local system/cache.
     *                              (default = false)
     * @param releaseLabel          Specifies a release label to be applied to the artifact(s)
     *                              being released
     * @param requestDetails        The {@link RequestDetails RequestDetails}
     * @return  The Bundle result containing the released resource(s)
     */
    @Operation(name = CRMI_OPERATION_RELEASE, idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = CRMI_OPERATION_RELEASE, value = "Release an existing draft artifact")
    public Bundle releaseOperation(
            @IdParam IdType id,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "versionBehavior") CodeType versionBehavior,
            @OperationParam(name = "latestFromTxServer", typeName = "Boolean")
                    IPrimitiveType<Boolean> latestFromTxServer,
            @OperationParam(name = "requireNonExperimental") CodeType requireNonExperimental,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "releaseLabel") StringType releaseLabel,
            RequestDetails requestDetails)
            throws FHIRException {
        return r4ReleaseServiceFactory
                .create(requestDetails)
                .release(
                        id,
                        getStringValue(version),
                        versionBehavior,
                        latestFromTxServer,
                        requireNonExperimental,
                        (Endpoint) getEndpoint(fhirVersion, terminologyEndpoint),
                        getStringValue(releaseLabel));
    }
}
