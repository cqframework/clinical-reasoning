package org.opencds.cqf.fhir.utility.adapter.dstu3

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.dstu3.model.*
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.opencds.cqf.fhir.utility.adapter.*

class AdapterFactory : IAdapterFactory {
    override fun createResource(resource: IBaseResource): IResourceAdapter {
        return when (resource) {
            is MetadataResource -> createKnowledgeArtifactAdapter(resource)
            is Endpoint -> createEndpoint(resource)
            is Parameters -> createParameters(resource)
            is Group -> createGroup(resource)
            else -> ResourceAdapter(resource as Resource)
        }
    }

    override fun createBase(element: IBase): IAdapter<*> {
        return when (element) {
            is IBaseResource -> createResource(element)
            is Questionnaire.QuestionnaireItemComponent -> createQuestionnaireItem(element)
            is QuestionnaireResponse.QuestionnaireResponseItemComponent ->
                createQuestionnaireResponseItem(element)
            is QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent ->
                createQuestionnaireResponseItemAnswer(element)
            is PlanDefinition.PlanDefinitionActionComponent -> createPlanDefinitionAction(element)
            is RequestGroup.RequestGroupActionComponent -> createRequestAction(element)
            is Parameters.ParametersParameterComponent -> createParametersParameter(element)
            else -> ElementAdapter(FhirVersionEnum.DSTU3, element)
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun createKnowledgeArtifactAdapter(
        resource: IDomainResource
    ): IKnowledgeArtifactAdapter {
        return when (resource) {
            is Library -> createLibrary(resource)
            is Measure -> MeasureAdapter(resource)
            is ActivityDefinition -> ActivityDefinitionAdapter(resource)
            is ImplementationGuide -> ImplementationGuideAdapter(resource)
            is PlanDefinition -> PlanDefinitionAdapter(resource)
            is Questionnaire -> QuestionnaireAdapter(resource)
            is StructureDefinition -> StructureDefinitionAdapter(resource)
            is ValueSet -> ValueSetAdapter(resource)
            is GraphDefinition -> GraphDefinitionAdapter(resource)
            is Group -> createGroup(resource)
            is MetadataResource -> KnowledgeArtifactAdapter(resource)
            else ->
                throw UnprocessableEntityException(
                    "Resource must be instance of ${MetadataResource::class.java.name}"
                )
        }
    }

    override fun createLibrary(library: IBaseResource): ILibraryAdapter {
        return LibraryAdapter(library as IDomainResource)
    }

    override fun createGroup(group: IBaseResource): IGroupAdapter {
        throw UnsupportedOperationException(
            "Groups a knowledge artifact are not supported in DSTU3 at this time."
        )
    }

    override fun createAttachment(attachment: IBase): IAttachmentAdapter {
        return AttachmentAdapter(attachment)
    }

    override fun createParameters(parameters: IBaseParameters): IParametersAdapter {
        return ParametersAdapter(parameters)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun createParametersParameter(
        parametersParametersComponent: IBase
    ): IParametersParameterComponentAdapter {
        return ParametersParameterComponentAdapter(parametersParametersComponent)
    }

    override fun createEndpoint(endpoint: IBaseResource): IEndpointAdapter {
        return EndpointAdapter(endpoint)
    }

    override fun createCodeableConcept(codeableConcept: IBase): ICodeableConceptAdapter {
        return CodeableConceptAdapter(codeableConcept)
    }

    override fun createCoding(coding: IBase): ICodingAdapter {
        return CodingAdapter(coding)
    }

    override fun createIdentifier(identifier: IBase): IIdentifierAdapter {
        return IdentifierAdapter(identifier)
    }

    override fun createElementDefinition(element: IBase): IElementDefinitionAdapter {
        return ElementDefinitionAdapter(element)
    }

    override fun createActivityDefinition(
        activityDefinition: IBaseResource
    ): IActivityDefinitionAdapter {
        return ActivityDefinitionAdapter(activityDefinition as IDomainResource)
    }

    override fun createPlanDefinition(planDefinition: IBaseResource): IPlanDefinitionAdapter {
        return PlanDefinitionAdapter(planDefinition as IDomainResource)
    }

    override fun createPlanDefinitionAction(action: IBase): IPlanDefinitionActionAdapter {
        return PlanDefinitionActionAdapter(action)
    }

    override fun createRequestAction(action: IBase): IRequestActionAdapter {
        return RequestActionAdapter(action)
    }

    override fun createDataRequirement(dataRequirement: IBase): IDataRequirementAdapter {
        return DataRequirementAdapter(dataRequirement)
    }

    override fun createQuestionnaire(questionnaire: IBaseResource): IQuestionnaireAdapter {
        return QuestionnaireAdapter(questionnaire as IDomainResource)
    }

    override fun createQuestionnaireItem(): IQuestionnaireItemComponentAdapter {
        return QuestionnaireItemComponentAdapter(Questionnaire.QuestionnaireItemComponent())
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun createQuestionnaireItem(item: IBase): IQuestionnaireItemComponentAdapter {
        return QuestionnaireItemComponentAdapter(item)
    }

    override fun createQuestionnaireResponse(
        questionnaireResponse: IBaseResource
    ): IQuestionnaireResponseAdapter {
        return QuestionnaireResponseAdapter(questionnaireResponse as IDomainResource)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun createQuestionnaireResponseItem(
        item: IBase
    ): IQuestionnaireResponseItemComponentAdapter {
        return QuestionnaireResponseItemComponentAdapter(item)
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun createQuestionnaireResponseItemAnswer(
        answer: IBase
    ): IQuestionnaireResponseItemAnswerComponentAdapter {
        return QuestionnaireResponseItemAnswerComponentAdapter(answer)
    }

    override fun createUsageContext(usageContext: IBase): IUsageContextAdapter {
        return UsageContextAdapter(usageContext)
    }

    override fun createValueSet(valueSet: IBaseResource): IValueSetAdapter {
        return ValueSetAdapter(valueSet as IDomainResource)
    }

    override fun createGraphDefinition(graphDefinition: IBaseResource): IGraphDefinitionAdapter {
        return GraphDefinitionAdapter(graphDefinition as IDomainResource)
    }

    override fun createStructureDefinition(
        structureDefinition: IBaseResource
    ): IStructureDefinitionAdapter {
        return StructureDefinitionAdapter(structureDefinition as IDomainResource)
    }

    override fun createImplementationGuide(
        implementationGuide: IBaseResource
    ): IImplementationGuideAdapter {
        return ImplementationGuideAdapter(implementationGuide as IDomainResource)
    }

    override fun createTuple(tuple: IBase): ITupleAdapter {
        throw UnprocessableEntityException("No FHIR type Tuple exists in version DSTU3")
    }
}
