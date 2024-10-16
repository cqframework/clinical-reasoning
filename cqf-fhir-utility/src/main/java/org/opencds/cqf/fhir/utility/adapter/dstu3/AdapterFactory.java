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
            return createKnowledgeArtifactAdapter((MetadataResource) resource);
        } else if (resource instanceof Endpoint) {
            return createEndpoint(resource);
        } else if (resource instanceof Parameters) {
            return createParameters((Parameters) resource);
        } else {
            return new ResourceAdapter(resource);
        }
    }

    @Override
    public IKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource resource) {
        IKnowledgeArtifactAdapter adapter;
        if (resource instanceof Library) {
            adapter = createLibrary(resource);
        } else if (resource instanceof Measure) {
            adapter = new MeasureAdapter((Measure) resource);
        } else if (resource instanceof PlanDefinition) {
            adapter = new PlanDefinitionAdapter((PlanDefinition) resource);
        } else if (resource instanceof Questionnaire) {
            adapter = new QuestionnaireAdapter((Questionnaire) resource);
        } else if (resource instanceof StructureDefinition) {
            adapter = new StructureDefinitionAdapter((StructureDefinition) resource);
        } else if (resource instanceof ValueSet) {
            adapter = new ValueSetAdapter((ValueSet) resource);
        } else {
            if (resource instanceof MetadataResource) {
                adapter = new KnowledgeArtifactAdapter((MetadataResource) resource);
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
}
