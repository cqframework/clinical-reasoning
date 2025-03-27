package org.opencds.cqf.fhir.cr.hapi.r4.plandefinition;

import static ca.uhn.fhir.rest.annotation.OperationParam.MAX_UNLIMITED;
import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S107")
public class PlanDefinitionApplyProvider {
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;

    public PlanDefinitionApplyProvider(IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
    }

    /**
     * Implements the <a href=
     * "http://www.hl7.org/fhir/plandefinition-operation-apply.html">$apply</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>. This implementation aims to be compatible with the
     * <a href="https://build.fhir.org/ig/HL7/cqf-recommendations/OperationDefinition-cpg-plandefinition-apply.html">
     * CPG IG</a>.
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
     * @param dataEndpoint        An endpoint to use to access data referenced by retrieve operations in libraries
     *                               referenced by the PlanDefinition.
     * @param contentEndpoint     An endpoint to use to access content (i.e. libraries) referenced by the PlanDefinition.
     * @param terminologyEndpoint An endpoint to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the PlanDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The CarePlan that is the result of applying the plan definition
     */
    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource apply(
            @IdParam IdType id,
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "encounter") String encounter,
            @OperationParam(name = "practitioner") String practitioner,
            @OperationParam(name = "organization") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource apply(
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject") String subject,
            @OperationParam(name = "encounter") String encounter,
            @OperationParam(name = "practitioner") String practitioner,
            @OperationParam(name = "organization") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }

    /**
     * Implements the <a href=
     * "http://www.hl7.org/fhir/plandefinition-operation-apply.html">$apply</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>. This implementation aims to be compatible with the
     * <a href="https://build.fhir.org/ig/HL7/cqf-recommendations/OperationDefinition-cpg-plandefinition-apply.html">
     * CPG IG</a>. This implementation follows the R5 specification and returns a bundle of RequestGroups rather than a CarePlan.
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
     * @param dataEndpoint        An endpoint to use to access data referenced by retrieve operations in libraries
     *                               referenced by the PlanDefinition.
     * @param contentEndpoint     An endpoint to use to access content (i.e. libraries) referenced by the PlanDefinition.
     * @param terminologyEndpoint An endpoint to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the PlanDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The Bundle that is the result of applying the plan definition
     */
    @Operation(name = ProviderConstants.CR_OPERATION_R5_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource applyR5(
            @IdParam IdType id,
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject", min = 1, max = MAX_UNLIMITED) List<String> subject,
            @OperationParam(name = "encounter") String encounter,
            @OperationParam(name = "practitioner") String practitioner,
            @OperationParam(name = "organization") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .applyR5(
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_R5_APPLY, idempotent = true, type = PlanDefinition.class)
    public IBaseResource applyR5(
            @OperationParam(name = "planDefinition") PlanDefinition planDefinition,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "subject", min = 1, max = MAX_UNLIMITED) List<String> subject,
            @OperationParam(name = "encounter") String encounter,
            @OperationParam(name = "practitioner") String practitioner,
            @OperationParam(name = "organization") String organization,
            @OperationParam(name = "userType") CodeableConcept userType,
            @OperationParam(name = "userLanguage") CodeableConcept userLanguage,
            @OperationParam(name = "userTaskContext") CodeableConcept userTaskContext,
            @OperationParam(name = "setting") CodeableConcept setting,
            @OperationParam(name = "settingContext") CodeableConcept settingContext,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            @OperationParam(name = "prefetchData") List<Parameters.ParametersParameterComponent> prefetchData,
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .applyR5(
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }
}
