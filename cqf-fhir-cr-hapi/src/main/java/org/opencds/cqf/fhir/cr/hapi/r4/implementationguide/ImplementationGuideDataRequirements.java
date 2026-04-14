package org.opencds.cqf.fhir.cr.hapi.r4.implementationguide;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;
import static org.opencds.cqf.fhir.utility.Resources.newBaseForVersion;
import static org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.forFhirVersion;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ImplementationGuideDataRequirements {
    private final IImplementationGuideProcessorFactory implementationGuideProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public ImplementationGuideDataRequirements(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        this.implementationGuideProcessorFactory = implementationGuideProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements the CRMI <a href="https://build.fhir.org/ig/HL7/crmi-ig/OperationDefinition-crmi-data-requirements.html">$data-requirements</a>
     * operation for ImplementationGuide resources (instance-level).
     *
     * @param id the id of the ImplementationGuide resource
     * @param artifactEndpointConfiguration configuration information to resolve canonical artifacts.
     *                                      Each element contains parts: artifactRoute, endpointUri, endpoint.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access
     *                            terminology (i.e. valuesets, codesystems, and membership testing)
     *                            referenced by the resource. If no terminology endpoint is supplied,
     *                            the evaluation will attempt to use the server on which the operation
     *                            is being performed as the terminology server.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a module-definition Library describing the data requirements
     */
    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "artifactEndpointConfiguration")
                    List<ParametersParameterComponent> artifactEndpointConfiguration,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.forMiddle3(id), buildParameters(artifactEndpointConfiguration, terminologyEndpoint));
    }

    /**
     * Implements the CRMI <a href="https://build.fhir.org/ig/HL7/crmi-ig/OperationDefinition-crmi-data-requirements.html">$data-requirements</a>
     * operation for ImplementationGuide resources (type-level).
     *
     * @param id the logical id of the ImplementationGuide resource to analyze
     * @param canonical a canonical reference to the ImplementationGuide resource
     * @param url canonical URL of the ImplementationGuide when invoked at the resource type level
     * @param version version of the ImplementationGuide when invoked at the resource type level
     * @param artifactEndpointConfiguration configuration information to resolve canonical artifacts.
     *                                      Each element contains parts: artifactRoute, endpointUri, endpoint.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access
     *                            terminology (i.e. valuesets, codesystems, and membership testing)
     *                            referenced by the resource. If no terminology endpoint is supplied,
     *                            the evaluation will attempt to use the server on which the operation
     *                            is being performed as the terminology server.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a module-definition Library describing the data requirements
     */
    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "canonical", typeName = "canonical") IPrimitiveType<String> canonical,
            @OperationParam(name = "url", typeName = "uri") IPrimitiveType<String> url,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "artifactEndpointConfiguration")
                    List<ParametersParameterComponent> artifactEndpointConfiguration,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.for3(
                                getCanonicalType(fhirVersion, canonical, url, version),
                                getIdType(fhirVersion, "ImplementationGuide", id),
                                null),
                        buildParameters(artifactEndpointConfiguration, terminologyEndpoint));
    }

    private IBaseParameters buildParameters(
            List<ParametersParameterComponent> artifactEndpointConfiguration,
            ParametersParameterComponent terminologyEndpoint) {
        if (artifactEndpointConfiguration == null && terminologyEndpoint == null) {
            return null;
        }
        var params = forFhirVersion(fhirVersion)
                .createParameters((IBaseParameters) newBaseForVersion("Parameters", fhirVersion));
        if (artifactEndpointConfiguration != null) {
            for (IBase config : artifactEndpointConfiguration) {
                params.addParameter(config);
            }
        }
        if (terminologyEndpoint != null) {
            var endpoint = getEndpoint(fhirVersion, terminologyEndpoint);
            if (endpoint != null) {
                params.addParameter("terminologyEndpoint", endpoint);
            }
        }
        return (IBaseParameters) params.get();
    }
}
