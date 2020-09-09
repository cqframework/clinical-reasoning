package org.opencds.cqf.cql.evaluator.fhir.adapter.dstu3;

import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;

@Named("DSTU3")
public class AdapterFactory implements org.opencds.cqf.cql.evaluator.fhir.adapter.AdapterFactory {

	@Override
	public org.opencds.cqf.cql.evaluator.fhir.adapter.ResourceAdapter createResource(IBaseResource resource) {

		return new ResourceAdapter(resource);
	}

	@Override
	public org.opencds.cqf.cql.evaluator.fhir.adapter.LibraryAdapter createLibrary(IBaseResource library) {
		return new LibraryAdapter(library);
	}

	@Override
	public org.opencds.cqf.cql.evaluator.fhir.adapter.AttachmentAdapter createAttachment(ICompositeType attachment) {
		return new AttachmentAdapter(attachment);
	}

	@Override
	public org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersAdapter createParameters(IBaseParameters parameters) {
		return new ParametersAdapter(parameters);
	}

	@Override
	public org.opencds.cqf.cql.evaluator.fhir.adapter.ParametersParameterComponentAdapter createParametersParameters(
			IBaseBackboneElement parametersParametersComponent) {
		return new ParametersParameterComponentAdapter(parametersParametersComponent);
	}

}
