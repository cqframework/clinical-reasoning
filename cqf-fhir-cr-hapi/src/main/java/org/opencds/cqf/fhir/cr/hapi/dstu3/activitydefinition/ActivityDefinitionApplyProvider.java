package org.opencds.cqf.fhir.cr.hapi.dstu3.activitydefinition;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.ActivityDefinition;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.hapi.common.IActivityDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S107")
public class ActivityDefinitionApplyProvider {
    private final IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory;

    public ActivityDefinitionApplyProvider(IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory) {
        this.activityDefinitionProcessorFactory = activityDefinitionProcessorFactory;
    }

    /**
     * Implements the <a href=
     * "http://www.hl7.org/fhir/activitydefinition-operation-apply.html">$apply</a>
     * operation found in the
     * <a href="http://www.hl7.org/fhir/clinicalreasoning-module.html">FHIR Clinical
     * Reasoning Module</a>.
     *
     * @param id                  The id of the ActivityDefinition to apply
     * @param canonical           The canonical identifier for the ActivityDefinition to apply (optionally version-specific)
     * @param activityDefinition  The ActivityDefinition to be applied
     * @param subject             The subject(s) that is/are the target of the activity definition to be applied.
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
     * @param parameters          Any input parameters defined in libraries referenced by the ActivityDefinition.
     * @param useServerData       Whether to use data from the server performing the evaluation.
     * @param data                Data to be made available to the ActivityDefinition evaluation.
     * @param dataEndpoint        An endpoint to use to access data referenced by retrieve operations in libraries
     *                               referenced by the ActivityDefinition.
     * @param contentEndpoint     An endpoint to use to access content (i.e. libraries) referenced by the ActivityDefinition.
     * @param terminologyEndpoint An endpoint to use to access terminology (i.e. valuesets, codesystems, and membership testing)
     *                               referenced by the ActivityDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                               autopopulated HAPI.
     * @return The resource that is the result of applying the definition
     */
    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = ActivityDefinition.class)
    public IBaseResource apply(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "activityDefinition") ActivityDefinition activityDefinition,
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
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return activityDefinitionProcessorFactory
                .create(requestDetails)
                .apply(
                        Eithers.for3(canonical == null ? null : new StringType(canonical), id, activityDefinition),
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = ActivityDefinition.class)
    public IBaseResource apply(
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "activityDefinition") ActivityDefinition activityDefinition,
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
            @OperationParam(name = "dataEndpoint") Endpoint dataEndpoint,
            @OperationParam(name = "contentEndpoint") Endpoint contentEndpoint,
            @OperationParam(name = "terminologyEndpoint") Endpoint terminologyEndpoint,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return activityDefinitionProcessorFactory
                .create(requestDetails)
                .apply(
                        Eithers.for3(canonical == null ? null : new StringType(canonical), null, activityDefinition),
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
                        dataEndpoint,
                        contentEndpoint,
                        terminologyEndpoint);
    }
}
