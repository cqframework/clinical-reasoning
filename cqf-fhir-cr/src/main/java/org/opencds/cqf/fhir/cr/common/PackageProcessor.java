package org.opencds.cqf.fhir.cr.common;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.visitor.PackageVisitor;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

@SuppressWarnings("UnstableApiUsage")
public class PackageProcessor implements IPackageProcessor {
    protected final IRepository repository;
    protected final CrSettings crSettings;
    protected final FhirVersionEnum fhirVersion;
    protected final PackageVisitor packageVisitor;
    protected final IAdapterFactory adapterFactory;

    public PackageProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public PackageProcessor(IRepository repository, CrSettings crSettings) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "crSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        packageVisitor = new PackageVisitor(this.repository, this.crSettings.getTerminologyServerClientSettings());
        adapterFactory = IAdapterFactory.forFhirVersion(fhirVersion);
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle) packageVisitor.visit(
                adapterFactory.createKnowledgeArtifactAdapter((IDomainResource) resource), parameters);
    }
}
