package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cr.visitor.RetireVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

public class RetireProcessor implements IRetireProcessor {

    private final IRepository repository;
    private final FhirVersionEnum fhirVersion;
    private final RetireVisitor visitor;
    private final IAdapterFactory adapterFactory;

    public RetireProcessor(IRepository repository) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        this.visitor = new RetireVisitor(repository);
        this.adapterFactory = IAdapterFactory.forFhirVersion(this.fhirVersion);
    }

    @Override
    public IBaseBundle retireResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle)
                visitor.visit(adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) resource), parameters);
    }
}
