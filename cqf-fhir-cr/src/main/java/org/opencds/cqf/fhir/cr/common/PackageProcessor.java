package org.opencds.cqf.fhir.cr.common;

import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.AdapterFactory;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactPackageVisitor;

public class PackageProcessor implements IPackageProcessor {
    protected final Repository repository;
    protected final FhirVersionEnum fhirVersion;
    protected final KnowledgeArtifactPackageVisitor packageVisitor;

    public PackageProcessor(Repository repository) {
        this.repository = repository;
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        packageVisitor = new KnowledgeArtifactPackageVisitor();
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource) {
        return packageResource(resource, "POST");
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, String method) {
        IBase[] parts = {};
        return packageResource(
                resource,
                newParameters(
                        repository.fhirContext(),
                        "package-parameters",
                        newBooleanPart(repository.fhirContext(), "isPut", method.equals("PUT"), parts)));
    }

    @Override
    public IBaseBundle packageResource(IBaseResource resource, IBaseParameters parameters) {
        return (IBaseBundle) packageVisitor.visit(
                AdapterFactory.forFhirVersion(fhirVersion).createKnowledgeArtifactAdapter((IDomainResource) resource),
                repository,
                parameters);
    }
}
