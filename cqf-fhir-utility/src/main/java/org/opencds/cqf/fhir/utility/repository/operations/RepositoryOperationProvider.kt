package org.opencds.cqf.fhir.utility.repository.operations

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import java.util.*
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ACTIVITY_DEFINITION
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CANONICAL
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CONTENT_ENDPOINT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA_ENDPOINT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ENCOUNTER
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ORGANIZATION
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PARAMETERS
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PLAN_DEFINITION
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PRACTITIONER
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SETTING_CONTEXT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SUBJECT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_TERMINOLOGY_ENDPOINT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_LANGUAGE
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TASK_CONTEXT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USER_TYPE
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_USE_SERVER_DATA
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory.Companion.forFhirVersion
import org.opencds.cqf.fhir.utility.monad.Eithers

class RepositoryOperationProvider(
    private val fhirContext: FhirContext,
    private val activityDefinitionProcessorFactory: IActivityDefinitionProcessorFactory?,
    private val planDefinitionProcessorFactory: IPlanDefinitionProcessorFactory?,
    private val questionnaireProcessorFactory: IQuestionnaireProcessorFactory?,
    private val questionnaireResponseProcessorFactory: IQuestionnaireResponseProcessorFactory?,
) : IRepositoryOperationProvider {
    private val noFactoryError = { a: String?, b: String? ->
        "No factory exists for ($a), unable to invoke operation ($b)"
    }
    private val unSupportedOpError = { a: String?, b: String? ->
        "($a) operation not supported for type ($b)"
    }
    private val activityDef = "ActivityDefinition"
    private val planDef = "PlanDefinition"
    private val questionnaire = "Questionnaire"
    private val questionnaireResponse = "QuestionnaireResponse"
    private val operationParametersParser: OperationParametersParser =
        OperationParametersParser(forFhirVersion(this.fhirContext.version.version))

    override fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeOperation(
        repository: IRepository,
        id: IIdType?,
        resourceType: String?,
        operationName: String?,
        parameters: IBaseParameters?,
    ): R? {
        val paramMap = operationParametersParser.getParameterParts(parameters)
        when (resourceType) {
            activityDef ->
                return invokeActivityDefinition<IPrimitiveType<String?>, R>(
                    repository,
                    id,
                    operationName,
                    paramMap,
                )

            else -> return null
        }
    }

    protected fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeActivityDefinition(
        repository: IRepository?,
        id: IIdType?,
        operationName: String?,
        paramMap: MutableMap<String?, Any?>?,
    ): R? {
        requireNotNull(activityDefinitionProcessorFactory) {
            noFactoryError(activityDef, operationName)
        }
        val processor = activityDefinitionProcessorFactory.create(repository)
        when (operationName) {
            "\$apply" -> {
                val activityDefinition =
                    Eithers.for3<C, IIdType, R>(
                        paramMap!![APPLY_PARAMETER_CANONICAL] as C?,
                        id,
                        paramMap[APPLY_PARAMETER_ACTIVITY_DEFINITION] as R?,
                    )
                val subject =
                    (paramMap[APPLY_PARAMETER_SUBJECT] as IPrimitiveType<String?>).getValue()
                val encounter = paramMap[APPLY_PARAMETER_ENCOUNTER] as IPrimitiveType<String?>?
                val practitioner =
                    paramMap[APPLY_PARAMETER_PRACTITIONER] as IPrimitiveType<String?>?
                val organization =
                    paramMap.get(APPLY_PARAMETER_ORGANIZATION) as IPrimitiveType<String?>?
                return processor!!.apply(
                    activityDefinition,
                    subject,
                    if (encounter == null) null else encounter.getValue(),
                    if (practitioner == null) null else practitioner.getValue(),
                    if (organization == null) null else organization.getValue(),
                    paramMap.get(APPLY_PARAMETER_USER_TYPE) as IBaseDatatype?,
                    paramMap.get(APPLY_PARAMETER_USER_LANGUAGE) as IBaseDatatype?,
                    paramMap.get(APPLY_PARAMETER_USER_TASK_CONTEXT) as IBaseDatatype?,
                    paramMap.get(APPLY_PARAMETER_SETTING) as IBaseDatatype?,
                    paramMap.get(APPLY_PARAMETER_SETTING_CONTEXT) as IBaseDatatype?,
                    paramMap.get(APPLY_PARAMETER_PARAMETERS) as IBaseParameters?,
                    (paramMap.get(APPLY_PARAMETER_USE_SERVER_DATA) as kotlin.Boolean?)!!,
                    paramMap.get(APPLY_PARAMETER_DATA) as IBaseBundle?,
                    paramMap.get(APPLY_PARAMETER_DATA_ENDPOINT) as IBaseResource?,
                    paramMap.get(APPLY_PARAMETER_CONTENT_ENDPOINT) as IBaseResource?,
                    paramMap.get(APPLY_PARAMETER_TERMINOLOGY_ENDPOINT) as IBaseResource?,
                ) as R?
            }

            else -> throw IllegalArgumentException(unSupportedOpError(operationName, activityDef))
        }
    }

    protected fun <C : IPrimitiveType<String?>, R : IBaseResource> invokePlanDefinition(
        repository: IRepository?,
        id: IIdType?,
        operationName: String,
        paramMap: MutableMap<String?, Any?>,
    ): R? {
        requireNotNull(planDefinitionProcessorFactory) { noFactoryError(planDef, operationName) }
        val processor = planDefinitionProcessorFactory.create(repository)
        when (operationName) {
            "\$apply",
            "\$r5.apply" -> {
                val planDefinition =
                    Eithers.for3(
                        paramMap[APPLY_PARAMETER_CANONICAL] as C?,
                        id,
                        paramMap[APPLY_PARAMETER_PLAN_DEFINITION] as R?,
                    )
                val subject =
                    (paramMap[APPLY_PARAMETER_SUBJECT] as IPrimitiveType<String?>).getValue()
                val encounterParam =
                    Optional.of(
                        (paramMap.get(APPLY_PARAMETER_ENCOUNTER)
                            as IPrimitiveType<kotlin.String?>?)!!
                    )
                val encounter =
                    if (encounterParam.isPresent) encounterParam.get().getValue() else null
                val practitionerParam =
                    Optional.of(
                        (paramMap[APPLY_PARAMETER_PRACTITIONER]
                            as IPrimitiveType<kotlin.String?>?)!!
                    )
                val practitioner =
                    if (practitionerParam.isPresent) practitionerParam.get().getValue() else null
                val organizationParam =
                    Optional.of(
                        (paramMap.get(APPLY_PARAMETER_ORGANIZATION)
                            as IPrimitiveType<kotlin.String?>?)!!
                    )
                val organization =
                    if (organizationParam.isPresent) organizationParam.get().getValue() else null
                val userType = paramMap[APPLY_PARAMETER_USER_TYPE] as IBaseDatatype?
                val userLanguage = paramMap[APPLY_PARAMETER_USER_LANGUAGE] as IBaseDatatype?
                val userTaskContext = paramMap[APPLY_PARAMETER_USER_TASK_CONTEXT] as IBaseDatatype?
                val setting = paramMap[APPLY_PARAMETER_SETTING] as IBaseDatatype?
                val settingContext = paramMap[APPLY_PARAMETER_SETTING_CONTEXT] as IBaseDatatype?
                val parameters = paramMap[APPLY_PARAMETER_PARAMETERS] as IBaseParameters?
                val useServerData = paramMap[APPLY_PARAMETER_USE_SERVER_DATA] as Boolean
                val data = paramMap[APPLY_PARAMETER_DATA] as IBaseBundle?
                val dataEndpoint = paramMap[APPLY_PARAMETER_DATA_ENDPOINT] as IBaseResource?
                val contentEndpoint = paramMap[APPLY_PARAMETER_CONTENT_ENDPOINT] as IBaseResource?
                val terminologyEndpoint =
                    paramMap[APPLY_PARAMETER_TERMINOLOGY_ENDPOINT] as IBaseResource?
                if (operationName == "\$r5.apply)") {
                    return processor!!.applyR5(
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
                        terminologyEndpoint,
                    ) as R?
                } else {
                    return processor!!.apply(
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
                        terminologyEndpoint,
                    ) as R?
                }
            }

            else -> throw IllegalArgumentException(unSupportedOpError(operationName, planDef))
        }
    }

    @Suppress("unused")
    protected fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeQuestionnaire(
        repository: IRepository?,
        id: IIdType?,
        operationName: String,
        paramMap: MutableMap<String?, Any?>?,
    ): R? {
        requireNotNull(questionnaireProcessorFactory) {
            noFactoryError(questionnaire, operationName)
        }
        val processor = questionnaireProcessorFactory.create(repository)
        when (operationName) {
            else -> throw IllegalArgumentException(unSupportedOpError(operationName, questionnaire))
        }
    }

    @Suppress("unused")
    protected fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeQuestionnaireResponse(
        repository: IRepository?,
        id: IIdType?,
        operationName: String,
        paramMap: MutableMap<String?, Any?>?,
    ): R? {
        requireNotNull(questionnaireResponseProcessorFactory) {
            noFactoryError(questionnaireResponse, operationName)
        }
        val processor = questionnaireResponseProcessorFactory.create(repository)
        when (operationName) {
            else ->
                throw IllegalArgumentException(
                    unSupportedOpError(operationName, questionnaireResponse)
                )
        }
    }
}
