package org.opencds.cqf.fhir.cr.common;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cr.visitor.PackageVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public class PackageProcessor implements IPackageProcessor {
    protected final IRepository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final PackageVisitor packageVisitor;
    protected final IAdapterFactory adapterFactory;

    public PackageProcessor(IRepository repository, TerminologyServerClientSettings terminologyServerClientSettings) {
        this.repository = repository;
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        packageVisitor = new PackageVisitor(this.repository, terminologyServerClientSettings);
        adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle) packageVisitor.visit(
                adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) resource), parameters);
    }
}
