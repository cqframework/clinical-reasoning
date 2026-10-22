package org.opencds.cqf.fhir.utility.repository

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.repository.IRepository
import org.hl7.fhir.instance.model.api.*
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ACTIVITY_DEFINITION
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CANONICAL
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_CONTENT_ENDPOINT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA_ENDPOINT
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ENCOUNTER
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ORGANIZATION
import org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PARAMETERS
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
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessorFactory
import org.opencds.cqf.fhir.utility.repository.operations.OperationParametersParser

class RepositoryOperationProvider(private val fhirContext: FhirContext) :
    IRepositoryOperationProvider {
    private val operationParametersParser: OperationParametersParser
    private var activityDefinitionProcessorFactory: IActivityDefinitionProcessorFactory? = null

    init {
        this.operationParametersParser =
            OperationParametersParser(forFhirVersion(this.fhirContext.getVersion().getVersion()))
    }

    fun setActivityDefinitionProcessorFactory(
        activityDefinitionProcessorFactory: IActivityDefinitionProcessorFactory?
    ) {
        this.activityDefinitionProcessorFactory = activityDefinitionProcessorFactory
    }

    override fun <C : IPrimitiveType<String?>, R : IBaseResource> invokeOperation(
        repository: IRepository?,
        id: IIdType?,
        resourceType: String?,
        operationName: String?,
        parameters: IBaseParameters?,
    ): R? {
        if (resourceType == "ActivityDefinition" && activityDefinitionProcessorFactory != null) {
            val processor = activityDefinitionProcessorFactory!!.create(repository)
            val paramMap = operationParametersParser.getParameterParts(parameters)
            when (operationName) {
                "\$apply" -> {
                    val activityDefinition =
                        Eithers.for3(
                            paramMap.get(APPLY_PARAMETER_CANONICAL) as C?,
                            id,
                            paramMap.get(APPLY_PARAMETER_ACTIVITY_DEFINITION) as R?,
                        )
                    val subject =
                        (paramMap.get(APPLY_PARAMETER_SUBJECT) as IPrimitiveType<String?>)
                            .getValue()
                    val encounter =
                        paramMap.get(APPLY_PARAMETER_ENCOUNTER) as IPrimitiveType<String?>?
                    val practitioner =
                        paramMap.get(APPLY_PARAMETER_PRACTITIONER) as IPrimitiveType<String?>?
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
                        paramMap.get(APPLY_PARAMETER_USE_SERVER_DATA) as Boolean,
                        paramMap.get(APPLY_PARAMETER_DATA) as IBaseBundle?,
                        paramMap.get(APPLY_PARAMETER_DATA_ENDPOINT) as IBaseResource?,
                        paramMap.get(APPLY_PARAMETER_CONTENT_ENDPOINT) as IBaseResource?,
                        paramMap.get(APPLY_PARAMETER_TERMINOLOGY_ENDPOINT) as IBaseResource?,
                    ) as R?
                }

                else ->
                    throw IllegalArgumentException(
                        "($operationName) operation not supported for type ($resourceType)"
                    )
            }
        }
        return null
    }
}
