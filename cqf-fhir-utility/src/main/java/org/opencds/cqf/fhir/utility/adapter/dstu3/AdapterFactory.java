package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.MetadataResource;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Questionnaire;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodeableConceptAdapter;
import org.opencds.cqf.fhir.utility.adapter.ICodingAdapter;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IResourceAdapter;

public class AdapterFactory implements IAdapterFactory {

    @Override
    public IResourceAdapter createResource(IBaseResource resource) {
        if (resource instanceof MetadataResource) {
            return createKnowledgeArtifactAdapter((IDomainResource) resource);
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
        } else if (resource instanceof PlanDefinition planDefinition) {
            adapter = new PlanDefinitionAdapter(planDefinition);
        } else if (resource instanceof Questionnaire questionnaire) {
            adapter = new QuestionnaireAdapter(questionnaire);
        } else if (resource instanceof StructureDefinition structureDefinition) {
            adapter = new StructureDefinitionAdapter(structureDefinition);
        } else if (resource instanceof ValueSet valueSet) {
            adapter = new ValueSetAdapter(valueSet);
        } else {
            if (resource instanceof MetadataResource metadataResource) {
                adapter = new KnowledgeArtifactAdapter(metadataResource);
            } else {
                throw new UnprocessableEntityException(
                        String.format("Resource must be instance of %s", MetadataResource.class.getName()));
            }
        }
        return adapter;
    }

    @Override
    public ILibraryAdapter createLibrary(IBaseResource library) {
        return new LibraryAdapter((IDomainResource) library);
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
    public IParametersParameterComponentAdapter createParametersParameters(
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
}
