package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.RequestGroup.RequestGroupActionComponent;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IImplementationGuideAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemAnswerComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITupleAdapter;
import org.opencds.cqf.fhir.utility.adapter.IUsageContextAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;

public class AdapterFactory implements IAdapterFactory {

    @Override
    public IResourceAdapter createResource(IBaseResource resource) {
        if (resource instanceof MetadataResource metadataResource) {
            return createKnowledgeArtifactAdapter(metadataResource);
        } else if (resource instanceof Endpoint) {
            return createEndpoint(resource);
        } else if (resource instanceof Parameters parameters) {
            return createParameters(parameters);
        } else {
            return new ResourceAdapter(resource);
        }
    }

    @Override
    public IAdapter<IBase> createBase(IBase element) {
        if (element instanceof QuestionnaireItemComponent item) {
            return createQuestionnaireItem(item);
        } else if (element instanceof QuestionnaireResponseItemComponent responseItem) {
            return createQuestionnaireResponseItem(responseItem);
        } else if (element instanceof QuestionnaireResponseItemAnswerComponent answer) {
            return createQuestionnaireResponseItemAnswer(answer);
        } else if (element instanceof PlanDefinitionActionComponent action) {
            return createPlanDefinitionAction(action);
        } else if (element instanceof RequestGroupActionComponent requestAction) {
            return createRequestAction(requestAction);
        } else if (element instanceof ParametersParameterComponent parametersParameterComponent) {
            return createParametersParameter(parametersParameterComponent);
        } else {
            throw new UnprocessableEntityException(
                    String.format("Unable to create an adapter for type: %s", element.fhirType()));
        }
    }

    @Override
    public IKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource resource) {
        IKnowledgeArtifactAdapter adapter;
        if (resource instanceof Library) {
            adapter = createLibrary(resource);
        } else if (resource instanceof Measure measure) {
            adapter = new MeasureAdapter(measure);
        } else if (resource instanceof ActivityDefinition activityDefinition) {
            adapter = new ActivityDefinitionAdapter(activityDefinition);
        } else if (resource instanceof ImplementationGuide implementationGuide) {
            adapter = new ImplementationGuideAdapter(implementationGuide);
        } else if (resource instanceof PlanDefinition planDefinition) {
            adapter = new PlanDefinitionAdapter(planDefinition);
        } else if (resource instanceof Questionnaire questionnaire) {
            adapter = new QuestionnaireAdapter(questionnaire);
        } else if (resource instanceof StructureDefinition structureDefinition) {
            adapter = new StructureDefinitionAdapter(structureDefinition);
        } else if (resource instanceof ValueSet valueSet) {
            adapter = new ValueSetAdapter(valueSet);
        } else if (resource instanceof GraphDefinition graphDefinition) {
            adapter = new GraphDefinitionAdapter(graphDefinition);
        } else {
            if (resource instanceof MetadataResource metadataResource) {
                adapter = new KnowledgeArtifactAdapter(metadataResource);
            } else {
                throw new UnprocessableEntityException(
                        "Resource must be instance of %s".formatted(MetadataResource.class.getName()));
            }
        }
        return adapter;
    }

    @Override
    public ILibraryAdapter createLibrary(IBaseResource library) {
        return new LibraryAdapter((IDomainResource) library);
    }

    @Override
    public IAttachmentAdapter createAttachment(IBase attachment) {
        return new AttachmentAdapter(attachment);
    }

    @Override
    public IParametersAdapter createParameters(IBaseParameters parameters) {
        return new ParametersAdapter(parameters);
    }

    @Override
    public IParametersParameterComponentAdapter createParametersParameter(IBase parametersParametersComponent) {
        return new ParametersParameterComponentAdapter(parametersParametersComponent);
    }

    @Override
    public IEndpointAdapter createEndpoint(IBaseResource endpoint) {
        return new EndpointAdapter(endpoint);
    }

    @Override
    public ICodeableConceptAdapter createCodeableConcept(IBase codeableConcept) {
        return new CodeableConceptAdapter(codeableConcept);
    }

    @Override
    public ICodingAdapter createCoding(IBase coding) {
        return new CodingAdapter(coding);
    }

    @Override
    public IElementDefinitionAdapter createElementDefinition(IBase element) {
        return new ElementDefinitionAdapter(element);
    }

    @Override
    public IActivityDefinitionAdapter createActivityDefinition(IBaseResource activityDefinition) {
        return new ActivityDefinitionAdapter((IDomainResource) activityDefinition);
    }

    @Override
    public IPlanDefinitionAdapter createPlanDefinition(IBaseResource planDefinition) {
        return new PlanDefinitionAdapter((IDomainResource) planDefinition);
    }

    @Override
    public IPlanDefinitionActionAdapter createPlanDefinitionAction(IBase action) {
        return new PlanDefinitionActionAdapter(action);
    }

    @Override
    public IRequestActionAdapter createRequestAction(IBase action) {
        return new RequestActionAdapter(action);
    }

    @Override
    public IDataRequirementAdapter createDataRequirement(IBase dataRequirement) {
        return new DataRequirementAdapter(dataRequirement);
    }

    @Override
    public IQuestionnaireAdapter createQuestionnaire(IBaseResource questionnaire) {
        return new QuestionnaireAdapter((IDomainResource) questionnaire);
    }

    @Override
    public IQuestionnaireItemComponentAdapter createQuestionnaireItem() {
        return new QuestionnaireItemComponentAdapter(new QuestionnaireItemComponent());
    }

    @Override
    public IQuestionnaireItemComponentAdapter createQuestionnaireItem(IBase item) {
        return new QuestionnaireItemComponentAdapter(item);
    }

    @Override
    public IQuestionnaireResponseAdapter createQuestionnaireResponse(IBaseResource questionnaireResponse) {
        return new QuestionnaireResponseAdapter((IDomainResource) questionnaireResponse);
    }

    @Override
    public IQuestionnaireResponseItemComponentAdapter createQuestionnaireResponseItem(IBase item) {
        return new QuestionnaireResponseItemComponentAdapter(item);
    }

    @Override
    public IQuestionnaireResponseItemAnswerComponentAdapter createQuestionnaireResponseItemAnswer(IBase answer) {
        return new QuestionnaireResponseItemAnswerComponentAdapter(answer);
    }

    @Override
    public IUsageContextAdapter createUsageContext(IBase usageContext) {
        return new UsageContextAdapter(usageContext);
    }

    @Override
    public IValueSetAdapter createValueSet(IBaseResource valueSet) {
        return new ValueSetAdapter((IDomainResource) valueSet);
    }

    @Override
    public IGraphDefinitionAdapter createGraphDefinition(IBaseResource graphDefinition) {
        return new GraphDefinitionAdapter((IDomainResource) graphDefinition);
    }

    @Override
    public IStructureDefinitionAdapter createStructureDefinition(IBaseResource structureDefinition) {
        return new StructureDefinitionAdapter((IDomainResource) structureDefinition);
    }

    @Override
    public IImplementationGuideAdapter createImplementationGuide(IBaseResource implementationGuide) {
        return new ImplementationGuideAdapter((IDomainResource) implementationGuide);
    }

    @Override
    public ITupleAdapter createTuple(IBase tuple) {
        return new TupleAdapter(tuple);
    }
}
