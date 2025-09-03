package org.opencds.cqf.fhir.cr.hapi.r4.graphdefinition;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.time.ZonedDateTime;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.graphdefintion.apply.ApplyRequestBuilder;
import org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionApplyRequestBuilderFactory;
import org.opencds.cqf.fhir.cr.hapi.common.IGraphDefinitionProcessorFactory;
import org.opencds.cqf.fhir.cr.hapi.common.StringTimePeriodHandler;

public class GraphDefinitionApplyProvider {

    private final IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory;
    private final IGraphDefinitionApplyRequestBuilderFactory graphDefinitionApplyRequestBuilderFactory;
    private final FhirVersionEnum fhirVersion;
    private final StringTimePeriodHandler stringTimePeriodHandler;

    public GraphDefinitionApplyProvider(
        IGraphDefinitionProcessorFactory graphDefinitionProcessorFactory,
        IGraphDefinitionApplyRequestBuilderFactory graphDefinitionApplyRequestBuilderFactory,
        FhirVersionEnum fhirVersion,
        StringTimePeriodHandler stringTimePeriodHandler) {

        this.graphDefinitionProcessorFactory = graphDefinitionProcessorFactory;
        this.graphDefinitionApplyRequestBuilderFactory = graphDefinitionApplyRequestBuilderFactory;
        this.fhirVersion = fhirVersion;
        this.stringTimePeriodHandler = stringTimePeriodHandler;
    }

    /**
     *
     * @param id                  The id of the GraphDefinition to apply
     * @param graphDefinition      The GraphDefinition to be applied
     * @param canonical           The canonical url of the plan definition to be applied. If the
     *                            operation is invoked at the instance level, this parameter is not
     *                            allowed; if the operation is invoked at the type level, this
     *                            parameter (and optionally the version), or the graphDefinition
     *                            parameter must be supplied.
     * @param url                 Canonical URL of the GraphDefinition when invoked at the resource
     *                            type level. This is exclusive with the graphDefinition and
     *                            canonical parameters.
     * @param version             Version of the GraphDefinition when invoked at the resource type
     *                            level. This is exclusive with the graphDefinition and canonical
     *                            parameters.
     * @param subject             The subject(s) that is/are the target of the plan definition to be
     *                            applied.
     * @param encounter           The encounter in context
     * @param practitioner        The practitioner in context
     * @param organization        The organization in context
     * @param userType            The type of user initiating the request, e.g. patient, healthcare
     *                            provider, or specific type of healthcare provider (physician,
     *                            nurse, etc.)
     * @param userLanguage        Preferred language of the person using the system
     * @param userTaskContext     The task the system user is performing, e.g. laboratory results
     *                            review, medication list review, etc. This information can be used
     *                            to tailor decision support outputs, such as recommended
     *                            information resources
     * @param setting             The current setting of the request (inpatient, outpatient, etc.)
     * @param settingContext      Additional detail about the setting of the request, if any
     * @param parameters          Any input parameters defined in libraries referenced by the
     *                            GraphDefinition.
     * @param useServerData       Whether to use data from the server performing the evaluation.
     * @param data                Data to be made available to the GraphDefinition evaluation.
     * @param dataEndpoint        The FHIR {@link Endpoint} Endpoint resource or url to use to
     *                            access data referenced by retrieve operations in libraries
     *                            referenced by the GraphDefinition.
     * @param contentEndpoint     The FHIR {@link Endpoint} Endpoint resource or url to use to
     *                            access content (i.e. libraries) referenced by the GraphDefinition.
     * @param terminologyEndpoint The FHIR {@link Endpoint} Endpoint resource or url to use to
     *                            access terminology (i.e. valuesets, codesystems, and membership
     *                            testing) referenced by the GraphDefinition.
     * @param requestDetails      The details (such as tenant) of this request. Usually
     *                            autopopulated HAPI.
     * @return The CarePlan that is the result of applying the plan definition
     */
    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = GraphDefinition.class)
    public IBaseResource apply(
        @IdParam IdType id,
        @OperationParam(name = "graphDefinition") GraphDefinition graphDefinition,
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
        @OperationParam(name = "prefetchData") List<ParametersParameterComponent> prefetchData,
        @OperationParam(name = "dataEndpoint") Parameters.ParametersParameterComponent dataEndpoint,
        @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
        @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
        @OperationParam(name = "periodStart") String periodStart,
        @OperationParam(name = "periodEnd") String periodEnd,
        RequestDetails requestDetails)
        throws InternalErrorException, FHIRException {

        var canonicalType = CanonicalHelper.getCanonicalType(fhirVersion, canonical, url, version);

        ApplyRequestBuilder applyRequestBuilder = graphDefinitionApplyRequestBuilderFactory
            .createApplyRequestBuilder(requestDetails)
            .withId(id)
            .withCanonicalType(canonicalType)
            .withGraphDefinition(graphDefinition)
            .withSubject(subject)
            .withUserType(userType)
            .withUserLanguage(userLanguage)
            .withUserTaskContext(userTaskContext)
            .withSetting(setting)
            .withSettingContext(settingContext)
            .withParameters(parameters)
            .withUseLocalData(useServerData == null ? true : useServerData.booleanValue())
            .withData(data)
            .withPrefetchData(prefetchData)
            .withDataEndpoint(dataEndpoint)
            .withContentEndpoint(contentEndpoint)
            .withTerminologyEndpoint(terminologyEndpoint)
            .withPeriodStart(getZonedStartDateTime(periodStart, requestDetails))
            .withPeriodEnd(getZonedEndDateTime(periodEnd, requestDetails));

        ApplyRequest applyRequest = applyRequestBuilder.buildApplyRequest();

        return graphDefinitionProcessorFactory
            .create(requestDetails)
            .apply(applyRequest);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_APPLY, idempotent = true, type = GraphDefinition.class)
    public IBaseResource apply(
        @OperationParam(name = "graphDefinition") GraphDefinition graphDefinition,
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
        @OperationParam(name = "dataEndpoint") Parameters.ParametersParameterComponent dataEndpoint,
        @OperationParam(name = "contentEndpoint") Parameters.ParametersParameterComponent contentEndpoint,
        @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
        @OperationParam(name = "periodStart") String periodStart,
        @OperationParam(name = "periodEnd") String periodEnd,
        RequestDetails requestDetails)
        throws InternalErrorException, FHIRException {

        var canonicalType = CanonicalHelper.getCanonicalType(fhirVersion, canonical, url, version);

        ApplyRequestBuilder applyRequestBuilder = graphDefinitionApplyRequestBuilderFactory
            .createApplyRequestBuilder(requestDetails)
            .withCanonicalType(canonicalType)
            .withGraphDefinition(graphDefinition)
            .withSubject(subject)
            .withUserType(userType)
            .withUserLanguage(userLanguage)
            .withUserTaskContext(userTaskContext)
            .withSetting(setting)
            .withSettingContext(settingContext)
            .withParameters(parameters)
            .withUseLocalData(useServerData == null ? true : useServerData.booleanValue())
            .withData(data)
            .withPrefetchData(prefetchData)
            .withDataEndpoint(dataEndpoint)
            .withContentEndpoint(contentEndpoint)
            .withTerminologyEndpoint(terminologyEndpoint)
            .withPeriodStart(getZonedStartDateTime(periodStart, requestDetails))
            .withPeriodEnd(getZonedEndDateTime(periodEnd, requestDetails));

        ApplyRequest applyRequest = applyRequestBuilder.buildApplyRequest();

        return graphDefinitionProcessorFactory
            .create(requestDetails)
            .apply(applyRequest);
    }

    protected ZonedDateTime getZonedStartDateTime(String start, RequestDetails requestDetails){
        return stringTimePeriodHandler.getStartZonedDateTime(start, requestDetails);
    }

    protected ZonedDateTime getZonedEndDateTime(String end, RequestDetails requestDetails){
        return stringTimePeriodHandler.getEndZonedDateTime(end, requestDetails);
    }
}
