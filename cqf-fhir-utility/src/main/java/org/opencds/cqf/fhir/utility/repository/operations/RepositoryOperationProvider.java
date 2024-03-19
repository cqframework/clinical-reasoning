package org.opencds.cqf.fhir.utility.repository.operations;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ACTIVITY_DEFINITION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CANONICAL;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CONTENT_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PLAN_DEFINITION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_TERMINOLOGY_ENDPOINT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_LANGUAGE;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TASK_CONTEXT;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TYPE;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USE_SERVER_DATA;

import ca.uhn.fhir.context.FhirContext;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class RepositoryOperationProvider implements IRepositoryOperationProvider {
    private final String noFactoryError = "No factory exists for (%s), unable to invoke operation (%s)";
    private final String unSupportedOpError = "(%s) operation not supported for type (%s)";
    private final String activityDef = "ActivityDefinition";
    private final String planDef = "PlanDefinition";
    private final String questionnaire = "Questionnaire";
    private final String questionnaireResponse = "QuestionnaireResponse";
    private final FhirContext fhirContext;
    private final OperationParametersParser operationParametersParser;
    private final IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory;
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;
    private final IQuestionnaireProcessorFactory questionnaireProcessorFactory;
    private final IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory;

    public RepositoryOperationProvider(
            FhirContext fhirContext,
            IActivityDefinitionProcessorFactory activityDefinitionProcessorFactory,
            IPlanDefinitionProcessorFactory planDefinitionProcessorFactory,
            IQuestionnaireProcessorFactory questionnaireProcessorFactory,
            IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory) {
        this.fhirContext = fhirContext;
        this.operationParametersParser = new OperationParametersParser(
                AdapterFactory.forFhirVersion(this.fhirContext.getVersion().getVersion()));
        this.activityDefinitionProcessorFactory = activityDefinitionProcessorFactory;
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
        this.questionnaireProcessorFactory = questionnaireProcessorFactory;
        this.questionnaireResponseProcessorFactory = questionnaireResponseProcessorFactory;
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeOperation(
            Repository repository, IIdType id, String resourceType, String operationName, IBaseParameters parameters) {
        requireNonNull(repository);
        var paramMap = operationParametersParser.getParameterParts(parameters);
        switch (resourceType) {
            case activityDef:
                return invokeActivityDefinition(repository, id, operationName, paramMap);

            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeActivityDefinition(
            Repository repository, IIdType id, String operationName, Map<String, Object> paramMap) {
        if (activityDefinitionProcessorFactory == null) {
            throw new IllegalArgumentException(String.format(noFactoryError, activityDef, operationName));
        }
        var processor = activityDefinitionProcessorFactory.create(repository);
        switch (operationName) {
            case "$apply":
                var activityDefinition = Eithers.for3((C) paramMap.get(APPLY_PARAMETER_CANONICAL), id, (R)
                        paramMap.get(APPLY_PARAMETER_ACTIVITY_DEFINITION));
                var subject = ((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_SUBJECT)).getValue();
                var encounter = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ENCOUNTER);
                var practitioner = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_PRACTITIONER);
                var organization = (IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ORGANIZATION);
                return (R) processor.apply(
                        activityDefinition,
                        subject,
                        encounter == null ? null : encounter.getValue(),
                        practitioner == null ? null : practitioner.getValue(),
                        organization == null ? null : organization.getValue(),
                        (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TYPE),
                        (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_LANGUAGE),
                        (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TASK_CONTEXT),
                        (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING),
                        (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING_CONTEXT),
                        (IBaseParameters) paramMap.get(APPLY_PARAMETER_PARAMETERS),
                        (Boolean) paramMap.get(APPLY_PARAMETER_USE_SERVER_DATA),
                        (IBaseBundle) paramMap.get(APPLY_PARAMETER_DATA),
                        (IBaseResource) paramMap.get(APPLY_PARAMETER_DATA_ENDPOINT),
                        (IBaseResource) paramMap.get(APPLY_PARAMETER_CONTENT_ENDPOINT),
                        (IBaseResource) paramMap.get(APPLY_PARAMETER_TERMINOLOGY_ENDPOINT));

            default:
                throw new IllegalArgumentException(String.format(unSupportedOpError, operationName, activityDef));
        }
    }

    @SuppressWarnings("unchecked")
    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R invokePlanDefinition(
            Repository repository, IIdType id, String operationName, Map<String, Object> paramMap) {
        if (planDefinitionProcessorFactory == null) {
            throw new IllegalArgumentException(String.format(noFactoryError, planDef, operationName));
        }
        var processor = planDefinitionProcessorFactory.create(repository);
        switch (operationName) {
            case "$apply":
            case "$r5.apply":
                var planDefinition = Eithers.for3((C) paramMap.get(APPLY_PARAMETER_CANONICAL), id, (R)
                        paramMap.get(APPLY_PARAMETER_PLAN_DEFINITION));
                var subject = ((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_SUBJECT)).getValue();
                var encounterParam = Optional.of((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ENCOUNTER));
                var encounter =
                        encounterParam.isPresent() ? encounterParam.get().getValue() : null;
                var practitionerParam =
                        Optional.of((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_PRACTITIONER));
                var practitioner =
                        practitionerParam.isPresent() ? practitionerParam.get().getValue() : null;
                var organizationParam =
                        Optional.of((IPrimitiveType<String>) paramMap.get(APPLY_PARAMETER_ORGANIZATION));
                var organization =
                        organizationParam.isPresent() ? organizationParam.get().getValue() : null;
                var userType = (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TYPE);
                var userLanguage = (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_LANGUAGE);
                var userTaskContext = (IBaseDatatype) paramMap.get(APPLY_PARAMETER_USER_TASK_CONTEXT);
                var setting = (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING);
                var settingContext = (IBaseDatatype) paramMap.get(APPLY_PARAMETER_SETTING_CONTEXT);
                var parameters = (IBaseParameters) paramMap.get(APPLY_PARAMETER_PARAMETERS);
                var useServerData = (Boolean) paramMap.get(APPLY_PARAMETER_USE_SERVER_DATA);
                var data = (IBaseBundle) paramMap.get(APPLY_PARAMETER_DATA);
                var dataEndpoint = (IBaseResource) paramMap.get(APPLY_PARAMETER_DATA_ENDPOINT);
                var contentEndpoint = (IBaseResource) paramMap.get(APPLY_PARAMETER_CONTENT_ENDPOINT);
                var terminologyEndpoint = (IBaseResource) paramMap.get(APPLY_PARAMETER_TERMINOLOGY_ENDPOINT);
                if (operationName.equals("$r5.apply)")) {
                    return (R) processor.applyR5(
                            planDefinition,
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
                            useServerData,
                            data,
                            dataEndpoint,
                            contentEndpoint,
                            terminologyEndpoint);
                } else {
                    return (R) processor.apply(
                            planDefinition,
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
                            useServerData,
                            data,
                            dataEndpoint,
                            contentEndpoint,
                            terminologyEndpoint);
                }

            default:
                throw new IllegalArgumentException(String.format(unSupportedOpError, operationName, planDef));
        }
    }

    @SuppressWarnings("unused")
    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeQuestionnaire(
            Repository repository, IIdType id, String operationName, Map<String, Object> paramMap) {
        if (questionnaireProcessorFactory == null) {
            throw new IllegalArgumentException(String.format(noFactoryError, questionnaire, operationName));
        }
        var processor = questionnaireProcessorFactory.create(repository);
        switch (operationName) {
            default:
                throw new IllegalArgumentException(String.format(unSupportedOpError, operationName, questionnaire));
        }
    }

    @SuppressWarnings("unused")
    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R invokeQuestionnaireResponse(
            Repository repository, IIdType id, String operationName, Map<String, Object> paramMap) {
        if (questionnaireResponseProcessorFactory == null) {
            throw new IllegalArgumentException(String.format(noFactoryError, questionnaireResponse, operationName));
        }
        var processor = questionnaireResponseProcessorFactory.create(repository);
        switch (operationName) {
            default:
                throw new IllegalArgumentException(
                        String.format(unSupportedOpError, operationName, questionnaireResponse));
        }
    }
}
