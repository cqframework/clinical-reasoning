package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;

public class AdapterFactory implements org.opencds.cqf.fhir.utility.adapter.AdapterFactory {

    @Override
    public org.opencds.cqf.fhir.utility.adapter.ResourceAdapter createResource(IBaseResource resource) {
        if (resource instanceof MetadataResource) {
            return createKnowledgeArtifactAdapter((IDomainResource) resource);
        } else {
            return new ResourceAdapter(resource);
        }
    }

    @Override
    public KnowledgeArtifactAdapter createKnowledgeArtifactAdapter(IDomainResource resource) {
        KnowledgeArtifactAdapter adapter;
        if (resource instanceof Library) {
            adapter = createLibrary(resource);
        } else if (resource instanceof PlanDefinition) {
            adapter = new org.opencds.cqf.fhir.utility.adapter.r4.PlanDefinitionAdapter((PlanDefinition) resource);
        } else if (resource instanceof Questionnaire) {
            adapter = new org.opencds.cqf.fhir.utility.adapter.r4.QuestionnaireAdapter((Questionnaire) resource);
        } else if (resource instanceof StructureDefinition) {
            adapter = new org.opencds.cqf.fhir.utility.adapter.r4.StructureDefinitionAdapter(
                    (StructureDefinition) resource);
        } else if (resource instanceof ValueSet) {
            adapter = new org.opencds.cqf.fhir.utility.adapter.r4.ValueSetAdapter((ValueSet) resource);
        } else {
            if (resource != null && resource instanceof MetadataResource) {
                adapter = new org.opencds.cqf.fhir.utility.adapter.r4.KnowledgeArtifactAdapter(
                        (MetadataResource) resource);
            } else {
                throw new UnprocessableEntityException(
                        String.format("Resource must be instance of %s", MetadataResource.class.getName()));
            }
        }
        return adapter;
    }

    @Override
    public LibraryAdapter createLibrary(IBaseResource library) {
        return new org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter((IDomainResource) library);
    }

    @Override
    public org.opencds.cqf.fhir.utility.adapter.AttachmentAdapter createAttachment(ICompositeType attachment) {
        return new AttachmentAdapter(attachment);
    }

    @Override
    public org.opencds.cqf.fhir.utility.adapter.ParametersAdapter createParameters(IBaseParameters parameters) {
        return new ParametersAdapter(parameters);
    }

    @Override
    public org.opencds.cqf.fhir.utility.adapter.ParametersParameterComponentAdapter createParametersParameters(
            IBaseBackboneElement parametersParametersComponent) {
        return new ParametersParameterComponentAdapter(parametersParametersComponent);
    }
}
