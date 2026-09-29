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
        if (resource is MetadataResource) {
            return createKnowledgeArtifactAdapter(resource)
        } else if (resource is Endpoint) {
            return createEndpoint(resource)
        } else if (resource is Parameters) {
            return createParameters(resource)
        } else if (resource is Group) {
            return createGroup(resource)
        } else {
            return ResourceAdapter(resource as Resource)
        }
    }

    override fun createBase(element: IBase): IAdapter<*> {
        if (element is IBaseResource) {
            return createResource(element)
        } else if (element is Questionnaire.QuestionnaireItemComponent) {
            return createQuestionnaireItem(element)
        } else if (element is QuestionnaireResponse.QuestionnaireResponseItemComponent) {
            return createQuestionnaireResponseItem(element)
        } else if (element is QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent) {
            return createQuestionnaireResponseItemAnswer(element)
        } else if (element is PlanDefinition.PlanDefinitionActionComponent) {
            return createPlanDefinitionAction(element)
        } else if (element is RequestGroup.RequestGroupActionComponent) {
            return createRequestAction(element)
        } else if (element is Parameters.ParametersParameterComponent) {
            return createParametersParameter(element)
        } else {
            return ElementAdapter(FhirVersionEnum.DSTU3, element)
        }
    }

    override fun createKnowledgeArtifactAdapter(
        resource: IDomainResource
    ): IKnowledgeArtifactAdapter {
        val adapter: IKnowledgeArtifactAdapter
        if (resource is Library) {
            adapter = createLibrary(resource)
        } else if (resource is Measure) {
            adapter = MeasureAdapter(resource)
        } else if (resource is ActivityDefinition) {
            adapter = ActivityDefinitionAdapter(resource)
        } else if (resource is ImplementationGuide) {
            adapter = ImplementationGuideAdapter(resource)
        } else if (resource is PlanDefinition) {
            adapter = PlanDefinitionAdapter(resource)
        } else if (resource is Questionnaire) {
            adapter = QuestionnaireAdapter(resource)
        } else if (resource is StructureDefinition) {
            adapter = StructureDefinitionAdapter(resource)
        } else if (resource is ValueSet) {
            adapter = ValueSetAdapter(resource)
        } else if (resource is GraphDefinition) {
            adapter = GraphDefinitionAdapter(resource)
        } else if (resource is Group) {
            adapter = createGroup(resource)
        } else {
            if (resource is MetadataResource) {
                adapter = KnowledgeArtifactAdapter(resource)
            } else {
                throw UnprocessableEntityException(
                    "Resource must be instance of ${MetadataResource::class.java.name}"
                )
            }
        }
        return adapter
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

    override fun createQuestionnaireItem(item: IBase): IQuestionnaireItemComponentAdapter {
        return QuestionnaireItemComponentAdapter(item)
    }

    override fun createQuestionnaireResponse(
        questionnaireResponse: IBaseResource
    ): IQuestionnaireResponseAdapter {
        return QuestionnaireResponseAdapter(questionnaireResponse as IDomainResource)
    }

    override fun createQuestionnaireResponseItem(
        item: IBase
    ): IQuestionnaireResponseItemComponentAdapter {
        return QuestionnaireResponseItemComponentAdapter(item)
    }

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
