package org.opencds.cqf.fhir.cr.hapi.r4.plandefinition;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.EndpointHelper.getEndpoint;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class PlanDefinitionPackageProvider {
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public PlanDefinitionPackageProvider(IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements a $package operation following the <a href="https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id the id of the Resource.
     * @param canonical the canonical identifier for the Resource (optionally version-specific).
     * @param url canonical URL of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param version version of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param usePut the boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = PlanDefinition.class)
    public IBaseBundle packagePlanDefinition(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        var params = packageParameters(
                fhirVersion, terminologyEndpointParam, usePut == null ? Boolean.FALSE : usePut.booleanValue());
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .packagePlanDefinition(Eithers.for3(canonicalType, id, null), params);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = PlanDefinition.class)
    public IBaseBundle packagePlanDefinition(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var idToUse = getIdType(fhirVersion, "PlanDefinition", id);
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        var params = packageParameters(
                fhirVersion, terminologyEndpointParam, usePut == null ? Boolean.FALSE : usePut.booleanValue());
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .packagePlanDefinition(Eithers.for3(canonicalType, idToUse, null), params);
    }
}
