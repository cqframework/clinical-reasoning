package org.opencds.cqf.fhir.utility.adapter.dstu3;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;

public class AdapterFactory implements org.opencds.cqf.fhir.utility.adapter.AdapterFactory {

    @Override
    public org.opencds.cqf.fhir.utility.adapter.ResourceAdapter createResource(IBaseResource resource) {

        return new ResourceAdapter(resource);
    }

    @Override
    public org.opencds.cqf.fhir.utility.adapter.LibraryAdapter createLibrary(IBaseResource library) {
        return new LibraryAdapter(library);
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
