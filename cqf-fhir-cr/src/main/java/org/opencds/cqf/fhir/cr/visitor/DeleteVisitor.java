package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.PackageHelper;
import org.opencds.cqf.fhir.utility.adapter.IKnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.search.Searches;

public class DeleteVisitor extends BaseKnowledgeArtifactVisitor {

    public DeleteVisitor(IRepository repository) {
        super(repository);
    }

    public static final String RETIRED_STATUS = "retired";

    @Override
    public IBase visit(IKnowledgeArtifactAdapter rootAdapter, IBaseParameters operationParams) {
        if (!rootAdapter.getStatus().equals(RETIRED_STATUS)) {
            throw new PreconditionFailedException("Cannot delete an artifact that is not in retired status");
        }

        var fhirVersion = rootAdapter.get().getStructureFhirVersionEnum();
        var transactionBundle = BundleHelper.newBundle(fhirVersion, null, "transaction");

        var resToUpdate = new ArrayList<IDomainResource>();
        resToUpdate.add(rootAdapter.get());

        var resourcesToUpdate = getComponents(rootAdapter, repository, resToUpdate);

        var resourceReference = rootAdapter.get().getIdElement().getResourceType() + "/"
                + rootAdapter.get().getIdElement().getIdPart();
        var approvals = retrieveApprovals(resourceReference);
        for (var approval : approvals) {
            resourcesToUpdate.add((IDomainResource) approval);
        }

        for (var res : resourcesToUpdate) {
            var entry = PackageHelper.deleteEntry(res);
            BundleHelper.addEntry(transactionBundle, entry);
        }

        return repository.transaction(transactionBundle);
    }

    private List<IBaseResource> retrieveApprovals(String resourceReference) {
        var searchParams = Searches.builder()
                .withReferenceParam("artifact", resourceReference)
                .build();
        return switch (fhirVersion()) {
            case DSTU3 ->
                BundleHelper.getEntryResources(repository.search(
                        org.hl7.fhir.dstu3.model.Bundle.class, org.hl7.fhir.dstu3.model.Basic.class, searchParams));
            case R4 ->
                BundleHelper.getEntryResources(repository.search(
                        org.hl7.fhir.r4.model.Bundle.class, org.hl7.fhir.r4.model.Basic.class, searchParams));
            case R5 ->
                BundleHelper.getEntryResources(repository.search(
                        org.hl7.fhir.r5.model.Bundle.class, org.hl7.fhir.r5.model.Basic.class, searchParams));
            default ->
                throw new UnprocessableEntityException("Unsupported version of FHIR: %s"
                        .formatted(fhirVersion().getFhirVersionString()));
        };
    }
}
