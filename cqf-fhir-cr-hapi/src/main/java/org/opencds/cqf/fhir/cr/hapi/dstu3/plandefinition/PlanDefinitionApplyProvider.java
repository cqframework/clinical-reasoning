package org.opencds.cqf.fhir.cr.hapi.dstu3.plandefinition;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S107")
public class PlanDefinitionApplyProvider {
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public PlanDefinitionApplyProvider(IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
        fhirVersion = FhirVersionEnum.DSTU3;
    }

    /**
     * Implements the <a href=
     * "http://www.hl7.org/fhir/plandefinition-operation-apply.html">$apply</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>.
     *
     * @param id                  The id of the PlanDefinition to apply
     * @param planDefinition      The PlanDefinition to be applied
     * @param canonical           The canonical url of the plan definition to be applied. If the operation is invoked at the instance level, this parameter is not allowed; if the operation is invoked at the type level, this parameter (and optionally the version), or the planDefinition parameter must be supplied.
     * @param url             	 Canonical URL of the PlanDefinition when invoked at the resource type level. This is exclusive with the planDefinition and canonical parameters.
     * @param version             Version of the PlanDefinition when invoked at the resource type level. This is exclusive with the planDefinition and canonical parameters.
     * @param subject             The subject(s) that is/are the target of the plan definition to be applied.
     * @param encounter           The encounter in context
     * @param practitioner        The practitioner in context
     * @param organization        The organization in context
     * @param userType            The type of user initiating the request, e.g. patient, healthcare provider,
     *                               or specific type of healthcare provider (physician, nurse, etc.)
     * @param userLanguage        Preferred language of the person using the system
     * @param userTaskContext     The task the system user is performing, e.g. laboratory results review,
     *                               medication list review, etc. This information can be used to tailor decision
     *                               support outputs, such as recommended information resources
     * @param setting             The current setting of the request (inpatient, outpatient, etc.)
     * @param settingContext      Additional detail about the setting of the request, if any
     * @param parameters          Any input parameters defined in libraries referenced by the PlanDefinition.
     * @param useServerData       Whether to use data from the server performing the evaluation.
     * @param data                Data to be made available to the PlanDefinition evaluation.
     * @param dataEndpoint        The FHIR {@link Endpoint} Endpoint resource or url to use to access data referenced by retrieve operations in libraries
     *                               referenced by the PlanDefinition.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to access content (i.e. libraries) referenced by the PlanDefinition.
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the PlanDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The CarePlan that is the result of applying the plan definition
     */
    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource apply(
            @IdParam IdType id,
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical", typeName = "string") String canonical,
            @OperationParam(name = "url", typeName = "string") String url,
            @OperationParam(name = "version", typeName = "string") String version,
            @OperationParam(name = "subject", typeName = "string") String subject,
            @OperationParam(name = "encounter", typeName = "string") String encounter,
            @OperationParam(name = "practitioner", typeName = "string") String practitioner,
            @OperationParam(name = "organization", typeName = "string") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var dataEndpointParam = getEndpoint(fhirVersion, dataEndpoint);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .apply(
                        Eithers.for3(canonicalType, id, planDefinition),
                        subject,
                        encounter,
                        practitioner,
                        organization,
                        userType,
                        userLanguage,
                        userTaskContext,
                        setting,
                        settingContext,
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        dataEndpointParam,
                        contentEndpointParam,
                        terminologyEndpointParam);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource apply(
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical", typeName = "string") String canonical,
            @OperationParam(name = "url", typeName = "string") String url,
            @OperationParam(name = "version", typeName = "string") String version,
            @OperationParam(name = "subject", typeName = "string") String subject,
            @OperationParam(name = "encounter", typeName = "string") String encounter,
            @OperationParam(name = "practitioner", typeName = "string") String practitioner,
            @OperationParam(name = "organization", typeName = "string") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") ParametersParameterComponent dataEndpoint,
            @OperationParam(name = "contentEndpoint") ParametersParameterComponent contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") ParametersParameterComponent terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        var canonicalType = getCanonicalType(fhirVersion, canonical, url, version);
        var dataEndpointParam = getEndpoint(fhirVersion, dataEndpoint);
        var contentEndpointParam = getEndpoint(fhirVersion, contentEndpoint);
        var terminologyEndpointParam = getEndpoint(fhirVersion, terminologyEndpoint);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .apply(
                        Eithers.for3(canonicalType, null, planDefinition),
                        subject,
                        encounter,
                        practitioner,
                        organization,
                        userType,
                        userLanguage,
                        userTaskContext,
                        setting,
                        settingContext,
                        parameters,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue(),
                        data,
                        prefetchData,
                        dataEndpointParam,
                        contentEndpointParam,
                        terminologyEndpointParam);
    }
}
