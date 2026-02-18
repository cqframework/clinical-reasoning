package org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class PlanDefinitionPackageProvider {
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public PlanDefinitionPackageProvider(IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
        fhirVersion = FhirVersionEnum.DSTU3;
    }

    /**
     * Implements a $package operation following the <a href="https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id the id of the Resource.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param usePut the boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = PlanDefinition.class)
    public IBaseBundle packagePlanDefinition(
            @IdParam IdType id,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .packagePlanDefinition(
                        Eithers.forMiddle3(id),
                        packageParameters(
                                fhirVersion,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                usePut == null ? Boolean.FALSE : usePut.booleanValue()));
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
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "canonical", typeName = "uri") IPrimitiveType<String> canonical,
            @OperationParam(name = "url", typeName = "uri") IPrimitiveType<String> url,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .packagePlanDefinition(
                        Eithers.for3(
                                getCanonicalType(fhirVersion, canonical, url, version),
                                getIdType(fhirVersion, "PlanDefinition", id),
                                null),
                        packageParameters(
                                fhirVersion,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                usePut == null ? Boolean.FALSE : usePut.booleanValue()));
    }
}
