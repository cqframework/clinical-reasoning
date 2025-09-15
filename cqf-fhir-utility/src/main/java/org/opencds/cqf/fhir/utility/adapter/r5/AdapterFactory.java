package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.ActivityDefinition;
import org.hl7.fhir.r5.model.Endpoint;
import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.MetadataResource;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.Questionnaire;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IRequestActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;
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
    public IKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource resource) {
        IKnowledgeArtifactAdapter adapter;
        if (resource instanceof Library) {
            adapter = createLibrary(resource);
        } else if (resource instanceof Measure measure) {
            adapter = new MeasureAdapter(measure);
        } else if (resource instanceof ActivityDefinition activityDefinition) {
            adapter = new ActivityDefinitionAdapter(activityDefinition);
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
    public IMeasureAdapter createMeasure(IBaseResource measure) {
        return new MeasureAdapter((IDomainResource) measure);
    }

    @Override
    public IAttachmentAdapter createAttachment(ICompositeType attachment) {
        return new AttachmentAdapter(attachment);
    }

    @Override
    public IParametersAdapter createParameters(IBaseParameters parameters) {
        return new ParametersAdapter(parameters);
    }

    @Override
    public IParametersParameterComponentAdapter createParametersParameter(
            IBaseBackboneElement parametersParametersComponent) {
        return new ParametersParameterComponentAdapter(parametersParametersComponent);
    }

    @Override
    public IEndpointAdapter createEndpoint(IBaseResource endpoint) {
        return new EndpointAdapter(endpoint);
    }

    @Override
    public ICodeableConceptAdapter createCodeableConcept(ICompositeType codeableConcept) {
        return new CodeableConceptAdapter(codeableConcept);
    }

    @Override
    public ICodingAdapter createCoding(ICompositeType coding) {
        return new CodingAdapter(coding);
    }

    @Override
    public IElementDefinitionAdapter createElementDefinition(ICompositeType element) {
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
    public IRequestActionAdapter createRequestAction(IBaseBackboneElement action) {
        return new RequestActionAdapter(action);
    }

    @Override
    public IDataRequirementAdapter createDataRequirement(ICompositeType dataRequirement) {
        return new DataRequirementAdapter(dataRequirement);
    }

    @Override
    public IQuestionnaireAdapter createQuestionnaire(IBaseResource questionnaire) {
        return new QuestionnaireAdapter((IDomainResource) questionnaire);
    }

    @Override
    public IValueSetAdapter createValueSet(IBaseResource valueSet) {
        return new ValueSetAdapter((IDomainResource) valueSet);
    }

    @Override
    public IGraphDefinitionAdapter createGraphDefinition(IBaseResource graphDefinition) {
        return new GraphDefinitionAdapter((IDomainResource) graphDefinition);
    }
}
