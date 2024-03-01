package org.opencds.cqf.fhir.utility.adapter.r4;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.adapter.IBaseKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;

public class AdapterFactory implements org.opencds.cqf.fhir.utility.adapter.AdapterFactory {

    @Override
    public org.opencds.cqf.fhir.utility.adapter.ResourceAdapter createResource(IBaseResource resource) {
        if (resource instanceof MetadataResource) {
            return createKnowledgeArtifactAdapter((MetadataResource) resource);
        } else {
            return new ResourceAdapter(resource);
        }
    }

    public IBaseKnowledgeArtifactAdapter createKnowledgeArtifactAdapter(MetadataResource resource) {
        IBaseKnowledgeArtifactAdapter retval;
        if (resource.fhirType().equals("Library")) {
            retval = createLibrary(resource);
        } else if (resource.fhirType().equals("PlanDefinition")) {
            retval = new org.opencds.cqf.fhir.utility.adapter.r4.PlanDefinitionAdapter((PlanDefinition) resource);
        } else if (resource.fhirType().equals("ValueSet")) {
            retval = new org.opencds.cqf.fhir.utility.adapter.r4.ValueSetAdapter((ValueSet) resource);
        } else {
            retval = new KnowledgeArtifactAdapter(resource);
        }
        return retval;
    }

    @Override
    public LibraryAdapter createLibrary(IBaseResource library) {
        return new r4LibraryAdapter(library);
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
