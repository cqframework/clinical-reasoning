package org.opencds.cqf.fhir.utility.adapter

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory

interface IAdapterFactory {
    /**
     * Creates an adapter that exposes common Resource operations across multiple versions of FHIR
     *
     * @param resource A FHIR Resource
     * @return an adapter exposing common api calls
     */
    fun createResource(resource: IBaseResource): IResourceAdapter

    /**
     * Creates an adapter that exposes common Resource operations across multiple versions of FHIR
     *
     * @param element A FHIR Base Element
     * @return an adapter exposing common api calls
     */
    fun createBase(element: IBase): IAdapter<*>

    /**
     * Creates an adapter that exposes common MetadataResource operations across multiple versions
     * of FHIR
     *
     * @param metadataResource A FHIR MetadataResource
     * @return an adapter exposing common api calls
     */
    fun createKnowledgeArtifactAdapter(metadataResource: IDomainResource): IKnowledgeArtifactAdapter

    /**
     * Creates an adapter that exposes common Library operations across multiple versions of FHIR
     *
     * @param library a FHIR Library Resource
     * @return an adapter exposing common api calls
     */
    fun createLibrary(library: IBaseResource): ILibraryAdapter

    /**
     * Creates an adapter that exposes common Group operations across multiple versions of FHIR
     *
     * @param group a FHIR Group Resource
     * @return an adapter exposing common api calls
     */
    fun createGroup(group: IBaseResource): IGroupAdapter

    /**
     * Creates an adapter that exposes common PlanDefinition operations across multiple versions of
     * FHIR
     *
     * @param planDefinition a FHIR PlanDefinition Resource
     * @return an adapter exposing common api calls
     */
    fun createPlanDefinition(planDefinition: IBaseResource): IPlanDefinitionAdapter

    /**
     * Creates an adapter that exposes common PlanDefinitionActionComponent operations across
     * multiple versions of FHIR
     *
     * @param action a FHIR PlanDefinitionActionComponent element
     * @return an adapter exposing common api calls
     */
    fun createPlanDefinitionAction(action: IBase): IPlanDefinitionActionAdapter

    /**
     * Creates an adapter that exposes common ActivityDefinition operations across multiple versions
     * of FHIR
     *
     * @param activityDefinition a FHIR ActivityDefinition Resource
     * @return an adapter exposing common api calls
     */
    fun createActivityDefinition(activityDefinition: IBaseResource): IActivityDefinitionAdapter

    /**
     * Creates an adapter that exposes common Attachment operations across multiple versions of FHIR
     *
     * @param attachment a FHIR Attachment Structure
     * @return an adapter exposing common api calls
     */
    fun createAttachment(attachment: IBase): IAttachmentAdapter

    /**
     * Creates an adapter that exposes common Parameters operations across multiple versions of FHIR
     *
     * @param parameters a FHIR Parameters Resource
     * @return an adapter exposing common api calls
     */
    fun createParameters(parameters: IBaseParameters): IParametersAdapter

    /**
     * Creates an adapter that exposes common ParametersParameterComponent operations across
     * multiple versions of FHIR
     *
     * @param parametersParameterComponent a FHIR ParametersParameterComponent Structure
     * @return an adapter exposing common api calls
     */
    fun createParametersParameter(
        parametersParameterComponent: IBase
    ): IParametersParameterComponentAdapter

    /**
     * Creates an adapter that exposes common Endpoint operations across multiple versions of FHIR
     *
     * @param endpoint a FHIR Endpoint Resource
     * @return an adapter exposing common api calls
     */
    fun createEndpoint(endpoint: IBaseResource): IEndpointAdapter

    /**
     * Creates an adapter that exposes common CodeableConcept operations across multiple versions of
     * FHIR
     *
     * @param codeableConcept a FHIR CodeableConcept object
     * @return an adapter exposing common api calls
     */
    fun createCodeableConcept(codeableConcept: IBase): ICodeableConceptAdapter

    /**
     * Creates an adapter that exposes common Coding operations across multiple versions of FHIR
     *
     * @param coding a FHIR Coding object
     * @return an adapter exposing common api calls
     */
    fun createCoding(coding: IBase): ICodingAdapter

    /**
     * Creates an adapter that exposes common Identifier operations across multiple versions of FHIR
     *
     * @param identifier a FHIR Identifier object
     * @return an adapter exposing common api calls
     */
    fun createIdentifier(identifier: IBase): IIdentifierAdapter

    /**
     * Creates an adapter that exposes common ElementDefinition operations across multiple versions
     * of FHIR
     *
     * @param element a FHIR ElementDefinition object
     * @return an adapter exposing common api calls
     */
    fun createElementDefinition(element: IBase): IElementDefinitionAdapter

    /**
     * Creates an adapter that exposes common RequestOrchestrationActionComponent operations across
     * multiple versions of FHIR
     *
     * @param action a FHIR RequestOrchestrationActionComponent object
     * @return an adapter exposing common api calls
     */
    fun createRequestAction(action: IBase): IRequestActionAdapter

    /**
     * Creates an adapter that exposes common DataRequirement operations across multiple versions of
     * FHIR
     *
     * @param dataRequirement a FHIR DataRequirement object
     * @return an adapter exposing common api calls
     */
    fun createDataRequirement(dataRequirement: IBase): IDataRequirementAdapter

    /**
     * Creates an adapter that exposes common Questionnaire operations across multiple versions of
     * FHIR
     *
     * @param questionnaire a FHIR Questionnaire object
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaire(questionnaire: IBaseResource): IQuestionnaireAdapter

    /**
     * Creates an adapter that exposes common Questionnaire item operations across multiple versions
     * of FHIR Includes a newly created QuestionnaireItemComponent of the appropriate version
     *
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaireItem(): IQuestionnaireItemComponentAdapter

    /**
     * Creates an adapter that exposes common Questionnaire item operations across multiple versions
     * of FHIR
     *
     * @param questionnaireItem a FHIR QuestionnaireItemComponent object
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaireItem(questionnaireItem: IBase): IQuestionnaireItemComponentAdapter

    /**
     * Creates an adapter that exposes common QuestionnaireResponse operations across multiple
     * versions of FHIR
     *
     * @param questionnaireResponse a FHIR QuestionnaireResponse object
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaireResponse(
        questionnaireResponse: IBaseResource
    ): IQuestionnaireResponseAdapter

    /**
     * Creates an adapter that exposes common QuestionnaireResponse item operations across multiple
     * versions of FHIR
     *
     * @param questionnaireResponseItem a FHIR QuestionnaireResponseItemComponent object
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaireResponseItem(
        questionnaireResponseItem: IBase
    ): IQuestionnaireResponseItemComponentAdapter

    /**
     * Creates an adapter that exposes common QuestionnaireResponse item answer operations across
     * multiple versions of FHIR
     *
     * @param questionnaireResponseItemAnswer a FHIR QuestionnaireResponseItemAnswerComponent object
     * @return an adapter exposing common api calls
     */
    fun createQuestionnaireResponseItemAnswer(
        questionnaireResponseItemAnswer: IBase
    ): IQuestionnaireResponseItemAnswerComponentAdapter

    /**
     * Creates an adapter that exposes common UsageContext operations across multiple versions of
     * FHIR
     *
     * @param usageContext a FHIR UsageContext object
     * @return an adapter exposing common api calls
     */
    fun createUsageContext(usageContext: IBase): IUsageContextAdapter

    /**
     * Creates an adapter that exposes common ValueSet operations across multiple versions of FHIR
     *
     * @param valueSet a FHIR ValueSet object
     * @return an adapter exposing common api calls
     */
    fun createValueSet(valueSet: IBaseResource): IValueSetAdapter

    /**
     * Creates an adapter that exposes common GraphDefinition operations across multiple versions of
     * FHIR
     *
     * @param graphDefinition a FHIR GraphDefinition Resource
     * @return an adapter exposing common api calls
     */
    fun createGraphDefinition(graphDefinition: IBaseResource): IGraphDefinitionAdapter

    /**
     * Creates an adapter that exposes common StructureDefinition operations across multiple
     * versions of FHIR
     *
     * @param structureDefinition a FHIR StructureDefinition Resource
     * @return an adapter exposing common api calls
     */
    fun createStructureDefinition(structureDefinition: IBaseResource): IStructureDefinitionAdapter

    /**
     * Creates an adapter that exposes common StructureDefinition operations across multiple
     * versions of FHIR
     *
     * @param implementationGuide a FHIR ImplementationGuide Resource
     * @return an adapter exposing common api calls
     */
    fun createImplementationGuide(implementationGuide: IBaseResource): IImplementationGuideAdapter

    /**
     * Creates an adapter that exposes common Tuple operations across multiple versions of FHIR
     *
     * @param tuple a HAPI FHIR Tuple object
     * @return an adapter exposing common api calls
     */
    fun createTuple(tuple: IBase): ITupleAdapter

    companion object {
        @JvmStatic
        fun forFhirContext(fhirContext: FhirContext): IAdapterFactory {
            return forFhirVersion(fhirContext.version.version)
        }

        @JvmStatic
        fun forFhirVersion(fhirVersion: FhirVersionEnum): IAdapterFactory {
            return when (fhirVersion) {
                FhirVersionEnum.DSTU3 -> AdapterFactory()
                FhirVersionEnum.R4 -> org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory()
                FhirVersionEnum.R5 -> org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory()
                else -> throw IllegalArgumentException("Unsupported FHIR version: $fhirVersion")
            }
        }

        /**
         * Creates an adapter that exposes common Resource operations across multiple versions of
         * FHIR
         *
         * @param resource A FHIR Resource
         * @return an adapter exposing common api calls
         */
        @JvmStatic
        fun createAdapterForResource(resource: IBaseResource): IResourceAdapter {
            return forFhirVersion(resource.structureFhirVersionEnum).createResource(resource)
        }

        /**
         * Creates an adapter that exposes common BackboneElement operations across multiple
         * versions of FHIR
         *
         * @param element A FHIR BaseBackboneElement
         * @return an adapter exposing common api calls
         */
        @JvmStatic
        fun createAdapterForBase(fhirVersion: FhirVersionEnum, element: IBase): IAdapter<*> {
            return forFhirVersion(fhirVersion).createBase(element)
        }
    }
}
