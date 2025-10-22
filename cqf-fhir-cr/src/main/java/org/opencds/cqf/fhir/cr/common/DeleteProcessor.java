package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cr.visitor.DeleteVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class DeleteProcessor implements IDeleteProcessor {

    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final DeleteVisitor visitor;
    protected final IAdapterFactory adapterFactory;

    public DeleteProcessor(IRepository repository) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        this.visitor = new DeleteVisitor(repository);
        this.adapterFactory = IAdapterFactory.forFhirVersion(this.fhirVersion);
    }

    @Override
    public IBaseBundle deleteResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle)
                visitor.visit(adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) resource), parameters);
    }
}
