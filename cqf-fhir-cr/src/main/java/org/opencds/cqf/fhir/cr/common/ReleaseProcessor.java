package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cr.visitor.ReleaseVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public class ReleaseProcessor implements IReleaseProcessor {
    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final ReleaseVisitor visitor;
    protected final IAdapterFactory adapterFactory;

    public ReleaseProcessor(IRepository repository) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        this.visitor = new ReleaseVisitor(this.repository, new TerminologyServerClientSettings());
        this.adapterFactory = IAdapterFactory.forFhirVersion(this.fhirVersion);
    }

    public ReleaseProcessor(IRepository repository, TerminologyServerClientSettings terminologyServerClientSettings) {
        this.repository = repository;
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        this.visitor = new ReleaseVisitor(this.repository, terminologyServerClientSettings);
        this.adapterFactory = IAdapterFactory.forFhirVersion(this.fhirVersion);
    }

    @Override
    public IBaseBundle releaseResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle)
                visitor.visit(adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) resource), parameters);
    }
}
